package com.backendemailservice.backendemailservice.service;

import com.backendemailservice.backendemailservice.dto.EmailResponseDto;
import com.backendemailservice.backendemailservice.dto.SendEmailRequestDto;
import com.backendemailservice.backendemailservice.entity.Email;
import com.backendemailservice.backendemailservice.entity.User;
import com.backendemailservice.backendemailservice.exception.EmailNotFoundException;
import com.backendemailservice.backendemailservice.exception.ReceiverNotFoundException;
import com.backendemailservice.backendemailservice.repository.EmailRepository;
import com.backendemailservice.backendemailservice.repository.UserRepository;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;


@Service
public class EmailService implements IEmailService {

    private final EmailRepository repository;
    private final CacheManager cacheManager;
    private final UserRepository userRepository;

    public EmailService(EmailRepository repository, CacheManager cacheManager, UserRepository userRepository) {
        this.repository = repository;
        this.cacheManager = cacheManager;
        this.userRepository = userRepository;
    }

    // --- New DTO-returning methods for controllers ---

    // DTO mapping in service layer, not controller
    @Override
    @Transactional(readOnly = true)
    public List<EmailResponseDto> loadInboxDtos(String userEmail) {
        return repository.loadInbox(userEmail).stream()
                .map(this::toDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<EmailResponseDto> loadOutboxDtos(String userEmail) {
        return repository.loadOutbox(userEmail).stream()
                .map(this::toDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<EmailResponseDto> loadTrashboxDtos(String userEmail) {
        return repository.loadTrashbox(userEmail).stream()
                .map(this::toDto)
                .toList();
    }

    // sendEmail handles entity construction + receiver check 
    @Override
    @Transactional
    public void sendEmail(String senderEmail, SendEmailRequestDto request) {
        if (userRepository.findByEmail(request.getReceiver()).isEmpty()) {
            throw new ReceiverNotFoundException("Receiver not found");
        }
        Email email = new Email();
        email.setSender(senderEmail);
        email.setReceiver(request.getReceiver());
        email.setSubject(request.getSubject());
        email.setBody(request.getBody());
        email.setPriority(request.getPriority());
        email.setDate(LocalDateTime.now());
        email.setTrash(false);
        repository.save(email);
    }

    // auth-check overload
    @Override
    @Transactional
    public void deleteEmail(Long emailID, String userEmail) {
        Email email = repository.findById(emailID)
                .orElseThrow(() -> new EmailNotFoundException("Email not found: " + emailID));
        repository.deleteById(emailID);
        evictInboxCache(userEmail);
    }

    // auth-check overload
    @Override
    @Transactional
    public void moveToTrashBox(Long emailID, String userEmail) {
        Email email = repository.findById(emailID)
                .orElseThrow(() -> new EmailNotFoundException("Email not found: " + emailID));
        repository.moveToTrashBox(emailID);
        evictInboxCache(userEmail);
    }

    // unified query with optional sort/filter params and mailbox context
    @Override
    @Transactional(readOnly = true)
    public List<EmailResponseDto> queryEmails(String email, String sort, String filterBy, String filterValue, String mailbox) {
        String box = (mailbox != null) ? mailbox : "Inbox";

        // Combined filter + sort — filter via repository, then sort in-memory
        if (sort != null && filterBy != null && filterValue != null) {
            List<Email> filtered = getFilteredEmails(email, filterBy, filterValue, box);
            sortEmailsInPlace(filtered, sort);
            return filtered.stream().map(this::toDto).toList();
        }

        if (sort != null) {
            if (sort.equals("priority")) {
                return switch (box) {
                    case "Outbox" -> repository.sortOutboxByPriority(email).stream().map(this::toDto).toList();
                    case "Trashbox" -> repository.sortTrashboxByPriority(email).stream().map(this::toDto).toList();
                    default -> repository.sortEmailsByPriority(email).stream().map(this::toDto).toList();
                };
            }
            if (sort.equals("date")) {
                return switch (box) {
                    case "Outbox" -> repository.sortOutboxByDate(email).stream().map(this::toDto).toList();
                    case "Trashbox" -> repository.sortTrashboxByDate(email).stream().map(this::toDto).toList();
                    default -> repository.sortEmailsByDate(email).stream().map(this::toDto).toList();
                };
            }
        }

        if (filterBy != null && filterValue != null) {
            if (filterBy.equals("subject")) {
                return switch (box) {
                    case "Outbox" -> repository.filterOutboxBySubject(email, filterValue).stream().map(this::toDto).toList();
                    case "Trashbox" -> repository.filterTrashBySubject(email, filterValue).stream().map(this::toDto).toList();
                    default -> repository.filterEmailsBySubject(email, filterValue).stream().map(this::toDto).toList();
                };
            }
            if (filterBy.equals("sender")) {
                return switch (box) {
                    // In Outbox, "sender" filter maps to receiver (all outbox items share the same sender)
                    case "Outbox" -> repository.filterOutboxByReceiver(email, filterValue).stream().map(this::toDto).toList();
                    case "Trashbox" -> repository.filterTrashBySender(email, filterValue).stream().map(this::toDto).toList();
                    default -> repository.filterEmailsBySender(email, filterValue).stream().map(this::toDto).toList();
                };
            }
        }

        // Default: return current mailbox
        return switch (box) {
            case "Outbox" -> loadOutboxDtos(email);
            case "Trashbox" -> loadTrashboxDtos(email);
            default -> loadInboxDtos(email);
        };
    }

    // --- Legacy methods ---

    // add @Transactional(readOnly = true) 
    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "inbox", key = "#user.getEmail()")
    public List<Email> loadInbox(User user) {
        return repository.loadInbox(user.getEmail());
    }

    // add @Transactional(readOnly = true) 
    @Override
    @Transactional(readOnly = true)
    public List<Email> loadOutbox(User user) {
        return repository.loadOutbox(user.getEmail());
    }

    // add @Transactional(readOnly = true) 
    @Override
    @Transactional(readOnly = true)
    public List<Email> loadTrashbox(User user) {
        return repository.loadTrashbox(user.getEmail());
    }

    // add @Transactional on write operations 
    @Override
    @Transactional
    @CacheEvict(value = "inbox", key = "#email.getReceiver()")
    public void createEmail(Email email) {
        repository.save(email);
    }

    // add @Transactional on write operations 
    @Override
    @Transactional
    public void deleteEmail(Long emailID) {
        Email email = repository.findById(emailID)
                .orElseThrow(() -> new EmailNotFoundException("Email not found: " + emailID));
        repository.deleteById(emailID);
        Cache cache = cacheManager.getCache("inbox");
        if (cache != null) {
            cache.evict(email.getReceiver());
        }
    }

    @Override
    @Transactional
    public void moveToTrashBox(Long emailID) {
        Email email = repository.findById(emailID)
                .orElseThrow(() -> new EmailNotFoundException("Email not found: " + emailID));
        repository.moveToTrashBox(emailID);
        Cache cache = cacheManager.getCache("inbox");
        if (cache != null) {
            cache.evict(email.getReceiver());
        }
    }

    // add @Transactional(readOnly = true) 
    @Override
    @Transactional(readOnly = true)
    public List<Email> sortEmails(String receiverEmail, String sortingOption) {
        if (sortingOption.equals("priority")) {
            return repository.sortEmailsByPriority(receiverEmail);
        } else {
            return repository.sortEmailsByDate(receiverEmail);
        }
    }

    // add @Transactional(readOnly = true) 
    @Override
    @Transactional(readOnly = true)
    public List<Email> filterEmails(String receiverEmail, String filteringOption, String filteringValue) {
        if (filteringOption.equals("subject")) {
            return repository.filterEmailsBySubject(receiverEmail, filteringValue);
        } else {
            return repository.filterEmailsBySender(receiverEmail, filteringValue);
        }
    }

    @Override
    @CacheEvict(value = "inbox", key = "#receiver")
    public void evictInboxCache(String receiver) {
        // Spring AOP handles eviction via annotation
    }

    // --- Internal helpers ---

    private List<Email> getFilteredEmails(String email, String filterBy, String filterValue, String box) {
        if (filterBy.equals("subject")) {
            return switch (box) {
                case "Outbox" -> repository.filterOutboxBySubject(email, filterValue);
                case "Trashbox" -> repository.filterTrashBySubject(email, filterValue);
                default -> repository.filterEmailsBySubject(email, filterValue);
            };
        }
        if (filterBy.equals("sender")) {
            return switch (box) {
                case "Outbox" -> repository.filterOutboxByReceiver(email, filterValue);
                case "Trashbox" -> repository.filterTrashBySender(email, filterValue);
                default -> repository.filterEmailsBySender(email, filterValue);
            };
        }
        throw new IllegalArgumentException("Unknown filterBy: " + filterBy);
    }

    private void sortEmailsInPlace(List<Email> emails, String sort) {
        if (sort.equals("priority")) {
            emails.sort((a, b) -> a.getPriority().compareTo(b.getPriority()));
        } else if (sort.equals("date")) {
            emails.sort((a, b) -> a.getDate().compareTo(b.getDate()));
        }
    }

    private EmailResponseDto toDto(Email email) {
        return new EmailResponseDto(
                email.getEmailID(),
                email.getSender(),
                email.getReceiver(),
                email.getSubject(),
                email.getBody(),
                email.getPriority(),
                email.getDate(),
                email.isTrash()
        );
    }

}
