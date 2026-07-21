package com.seamail.mail.service;

import com.seamail.mail.dto.EmailResponseDto;
import com.seamail.mail.dto.SendEmailRequestDto;
import com.seamail.mail.entity.Email;
import com.seamail.mail.entity.Mailbox;
import com.seamail.mail.exception.EmailNotFoundException;
import com.seamail.mail.exception.ReceiverNotFoundException;
import com.seamail.mail.repository.EmailRepository;
import com.seamail.mail.client.AuthUserClient;
import feign.FeignException;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;


@Service
public class EmailService implements IEmailService {

    private final EmailRepository repository;
    private final AuthUserClient authUserClient;

    public EmailService(EmailRepository repository, AuthUserClient authUserClient) {
        this.repository = repository;
        this.authUserClient = authUserClient;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<EmailResponseDto> loadInboxDtos(String userEmail, Pageable pageable) {
        return repository.loadInbox(userEmail, pageable).map(this::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<EmailResponseDto> loadOutboxDtos(String userEmail, Pageable pageable) {
        return repository.loadOutbox(userEmail, pageable).map(this::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<EmailResponseDto> loadTrashboxDtos(String userEmail, Pageable pageable) {
        return repository.loadTrashbox(userEmail, pageable).map(this::toDto);
    }

    @Override
    @Transactional
    @CacheEvict(value = "inbox", allEntries = true)
    public void sendEmail(String senderEmail, SendEmailRequestDto request) {
        try {
            authUserClient.assertUserExists(request.getReceiver());
        } catch (FeignException.NotFound ex) {
            throw new ReceiverNotFoundException("Receiver not found");
        } catch (FeignException ex) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Authentication service unavailable");
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

    @Override
    @Transactional
    @CacheEvict(value = "inbox", allEntries = true)
    public void deleteEmail(Long emailID, String userEmail) {
        Email email = repository.findById(emailID)
                .orElseThrow(() -> new EmailNotFoundException("Email not found: " + emailID));
        if (!email.getReceiver().equals(userEmail)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized");
        }
        if (!email.isTrash()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Email must be moved to trash before it can be permanently deleted");
        }
        repository.deleteById(emailID);
    }

    @Override
    @Transactional
    @CacheEvict(value = "inbox", allEntries = true)
    public void moveToTrashBox(Long emailID, String userEmail) {
        Email email = repository.findById(emailID)
                .orElseThrow(() -> new EmailNotFoundException("Email not found: " + emailID));
        if (!email.getReceiver().equals(userEmail)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized");
        }
        repository.moveToTrashBox(emailID, userEmail);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<EmailResponseDto> queryEmails(String email, String sort, String filterBy,
                                              String filterValue, String mailbox, Pageable pageable) {
        Mailbox box = Mailbox.fromString(mailbox);

        if (sort != null && filterBy != null && filterValue != null) {
            Page<Email> filtered = getFilteredEmails(email, filterBy, filterValue, box, pageable);
            Page<Email> sorted = filterAndSort(filtered, sort);
            return sorted.map(this::toDto);
        }

        if (sort != null) {
            if (sort.equals("priority")) {
                return switch (box) {
                    case OUTBOX -> repository.sortOutboxByPriority(email, pageable).map(this::toDto);
                    case TRASHBOX -> repository.sortTrashboxByPriority(email, pageable).map(this::toDto);
                    default -> repository.sortEmailsByPriority(email, pageable).map(this::toDto);
                };
            }
            if (sort.equals("date")) {
                return switch (box) {
                    case OUTBOX -> repository.sortOutboxByDate(email, pageable).map(this::toDto);
                    case TRASHBOX -> repository.sortTrashboxByDate(email, pageable).map(this::toDto);
                    default -> repository.sortEmailsByDate(email, pageable).map(this::toDto);
                };
            }
        }

        if (filterBy != null && filterValue != null) {
            if (filterBy.equals("subject")) {
                return switch (box) {
                    case OUTBOX -> repository.filterOutboxBySubject(email, filterValue, pageable).map(this::toDto);
                    case TRASHBOX -> repository.filterTrashBySubject(email, filterValue, pageable).map(this::toDto);
                    default -> repository.filterEmailsBySubject(email, filterValue, pageable).map(this::toDto);
                };
            }
            if (filterBy.equals("sender")) {
                return switch (box) {
                    case OUTBOX -> repository.filterOutboxByReceiver(email, filterValue, pageable).map(this::toDto);
                    case TRASHBOX -> repository.filterTrashBySender(email, filterValue, pageable).map(this::toDto);
                    default -> repository.filterEmailsBySender(email, filterValue, pageable).map(this::toDto);
                };
            }
        }

        return switch (box) {
            case OUTBOX -> loadOutboxDtos(email, pageable);
            case TRASHBOX -> loadTrashboxDtos(email, pageable);
            default -> loadInboxDtos(email, pageable);
        };
    }

    private Page<Email> getFilteredEmails(String email, String filterBy, String filterValue, Mailbox box, Pageable pageable) {
        if (filterBy.equals("subject")) {
            return switch (box) {
                case OUTBOX -> repository.filterOutboxBySubject(email, filterValue, pageable);
                case TRASHBOX -> repository.filterTrashBySubject(email, filterValue, pageable);
                default -> repository.filterEmailsBySubject(email, filterValue, pageable);
            };
        }
        if (filterBy.equals("sender")) {
            return switch (box) {
                case OUTBOX -> repository.filterOutboxByReceiver(email, filterValue, pageable);
                case TRASHBOX -> repository.filterTrashBySender(email, filterValue, pageable);
                default -> repository.filterEmailsBySender(email, filterValue, pageable);
            };
        }
        throw new IllegalArgumentException("Unknown filterBy: " + filterBy);
    }

    private Page<Email> filterAndSort(Page<Email> page, String sort) {
        List<Email> content = new ArrayList<>(page.getContent());
        if (sort.equals("priority")) {
            content.sort((a, b) -> a.getPriority().compareTo(b.getPriority()));
        } else if (sort.equals("date")) {
            content.sort((a, b) -> a.getDate().compareTo(b.getDate()));
        }
        return new org.springframework.data.domain.PageImpl<>(content, page.getPageable(), page.getTotalElements());
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
