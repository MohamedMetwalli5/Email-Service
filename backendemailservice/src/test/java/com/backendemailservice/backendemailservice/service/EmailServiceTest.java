package com.backendemailservice.backendemailservice.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.test.context.ActiveProfiles;

import com.backendemailservice.backendemailservice.entity.Email;
import com.backendemailservice.backendemailservice.entity.User;
import com.backendemailservice.backendemailservice.repository.EmailRepository;
import com.backendemailservice.backendemailservice.repository.UserRepository;

@SpringBootTest
@ActiveProfiles("test")
public class EmailServiceTest {

    @Autowired
    private EmailService emailService;

    @Autowired
    private EmailRepository emailRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CacheManager cacheManager;

    @BeforeEach
    public void setUp() {
        emailRepository.deleteAll();
        userRepository.deleteAll();
        cacheManager.getCacheNames()
                .forEach(name -> cacheManager.getCache(name).clear());
    }

    @Test
    public void testCreateEmail() {
        User user = new User("test@seamail.com", "password");
        userRepository.save(user);

        Email email = new Email(user.getEmail(), "recipient@seamail.com", "Test Email", "This is a test email.", "1", LocalDateTime.now(), false);
        emailService.createEmail(email);

        assertThat(emailRepository.count()).isEqualTo(1);

        // Validating the details of the saved email
        Email savedEmail = emailRepository.findById(email.getEmailID()).orElse(null);
        assertThat(savedEmail).isNotNull();
        assertThat(savedEmail.getSender()).isEqualTo(user.getEmail());
        assertThat(savedEmail.getReceiver()).isEqualTo("recipient@seamail.com");
        assertThat(savedEmail.getSubject()).isEqualTo("Test Email");
        assertThat(savedEmail.getBody()).isEqualTo("This is a test email.");
        assertThat(savedEmail.getPriority()).isEqualTo("1");
        assertThat(savedEmail.getDate()).isNotNull();
        assertThat(savedEmail.isTrash()).isEqualTo(false);
    }

    @Test
    public void testLoadInbox() {
        User user = new User("test@seamail.com", "password");
        userRepository.save(user);

        Email email = new Email("sender@seamail.com", user.getEmail(), "Test Email", "This is a test email.", "1", LocalDateTime.now(), false);
        emailRepository.save(email);

        List<Email> inboxEmails = emailService.loadInbox(user);
        assertThat(inboxEmails).isNotNull();
        assertThat(inboxEmails.size()).isEqualTo(1);
        assertThat(inboxEmails.get(0).getSubject()).isEqualTo("Test Email");
    }

    @Test
    public void testLoadOutbox() {
        User user = new User("test@seamail.com", "password");
        userRepository.save(user);

        Email email = new Email(user.getEmail(), "recipient@seamail.com", "Test Email", "This is a test email.", "1", LocalDateTime.now(), false);
        emailService.createEmail(email);

        List<Email> outboxEmails = emailService.loadOutbox(user);
        assertThat(outboxEmails).isNotNull();
        assertThat(outboxEmails.size()).isEqualTo(1);
        assertThat(outboxEmails.get(0).getSubject()).isEqualTo("Test Email");
    }

    @Test
    public void testLoadTrashbox() {
        User user = new User("test@seamail.com", "password");
        userRepository.save(user);

        Email email = new Email("sender@seamail.com", user.getEmail(), "Test Email", "This is a test email.", "1", LocalDateTime.now(), true);
        emailService.createEmail(email);

        List<Email> trashedEmails = emailService.loadTrashbox(user);
        assertThat(trashedEmails).isNotNull();
        assertThat(trashedEmails.size()).isEqualTo(1);
        assertThat(trashedEmails.get(0).getSubject()).isEqualTo("Test Email");
    }

    @Test
    public void testDeleteEmail() {
        User user = new User("test@seamail.com", "password");
        userRepository.save(user);

        Email email = new Email(user.getEmail(), "recipient@seamail.com", "Test Email", "This is a test email.", "1", LocalDateTime.now(), false);
        emailService.createEmail(email);

        assertThat(emailRepository.count()).isEqualTo(1);

        emailService.deleteEmail(email.getEmailID());

        assertThat(emailRepository.count()).isEqualTo(0);
    }

    @Test
    public void testSortEmailsByPriority() {
        User user = new User("test@seamail.com", "password");
        userRepository.save(user);

        Email email1 = new Email("sender@seamail.com", user.getEmail(), "Test Email 1", "First email", "2", LocalDateTime.now(), false);
        Email email2 = new Email("sender@seamail.com", user.getEmail(), "Test Email 2", "Second email", "1", LocalDateTime.now(), false);
        emailService.createEmail(email1);
        emailService.createEmail(email2);

        List<Email> userEmails = emailRepository.loadInbox(user.getEmail());
        List<Email> sortedEmails = emailRepository.sortEmailsByPriority(user.getEmail());

        assertThat(sortedEmails).isNotNull();
        assertThat(sortedEmails.size()).isEqualTo(userEmails.size());
    }

    @Test
    public void testFilterEmailsBySubject() {
        User user = new User("test@seamail.com", "password");
        userRepository.save(user);

        Email email1 = new Email("sender@seamail.com", user.getEmail(), "Important Test Email", "First email", "1", LocalDateTime.now(), false);
        Email email2 = new Email("sender@seamail.com", user.getEmail(), "Another Test Email", "Second email", "1", LocalDateTime.now(), false);
        emailService.createEmail(email1);
        emailService.createEmail(email2);

        List<Email> filteredEmails = emailService.filterEmails(user.getEmail(), "subject", "Important Test Email");

        assertThat(filteredEmails).isNotNull();
        assertThat(filteredEmails.size()).isEqualTo(1);
        assertThat(filteredEmails.get(0).getSubject()).isEqualTo("Important Test Email");
    }

    @Test
    public void testFilterEmailsBySender() {
        User user = new User("test@seamail.com", "password");
        userRepository.save(user);

        Email email1 = new Email("sender1@seamail.com", user.getEmail(), "Test Email 1", "First email", "1", LocalDateTime.now(), false);
        Email email2 = new Email("sender2@seamail.com", user.getEmail(), "Test Email 2", "Second email", "1", LocalDateTime.now(), false);
        emailService.createEmail(email1);
        emailService.createEmail(email2);

        List<Email> filteredEmails = emailService.filterEmails(user.getEmail(), "sender", "sender1@seamail.com");

        assertThat(filteredEmails).isNotNull();
        assertThat(filteredEmails.size()).isEqualTo(1);
        assertThat(filteredEmails.get(0).getSender()).isEqualTo("sender1@seamail.com");
    }

    @Test
    public void testMoveToTrashBox() {
        User user = new User("test@seamail.com", "password");
        userRepository.save(user);

        Email email = new Email("sender@seamail.com", user.getEmail(), "Test Email", "This is a test email.", "1", LocalDateTime.now(), false);
        emailService.createEmail(email);

        emailService.moveToTrashBox(email.getEmailID());

        Email trashedEmail = emailRepository.findById(email.getEmailID()).orElse(null);
        assertThat(trashedEmail).isNotNull();
        assertThat(trashedEmail.isTrash()).isEqualTo(true); // Checking for the updated trash value
    }

    @Test
    public void testLoadInbox_IsCached() {
        User user = new User("test@seamail.com", "password");
        userRepository.save(user);

        Email email = new Email("sender@seamail.com", user.getEmail(), "Subject", "Body", "1", LocalDateTime.now(), false);
        emailRepository.save(email);

        List<Email> firstCall = emailService.loadInbox(user);
        List<Email> secondCall = emailService.loadInbox(user);

        assertThat(firstCall).isEqualTo(secondCall);
        assertThat(cacheManager.getCache("inbox").get(user.getEmail())).isNotNull();
    }

    @Test
    public void testLoadInbox_CacheEvictedOnCreateEmail() {
        User user = new User("test@seamail.com", "password");
        userRepository.save(user);

        Email email = new Email("sender@seamail.com", user.getEmail(), "Subject", "Body", "1", LocalDateTime.now(), false);
        emailRepository.save(email);

        emailService.loadInbox(user);
        assertThat(cacheManager.getCache("inbox").get(user.getEmail())).isNotNull();

        Email newEmail = new Email("sender2@seamail.com", user.getEmail(), "New", "Body", "1", LocalDateTime.now(), false);
        emailService.createEmail(newEmail);

        assertThat(cacheManager.getCache("inbox").get(user.getEmail())).isNull();
    }

    @Test
    public void testLoadInbox_CacheEvictedOnMoveToTrash() {
        User user = new User("test@seamail.com", "password");
        userRepository.save(user);

        Email email = new Email("sender@seamail.com", user.getEmail(), "Subject", "Body", "1", LocalDateTime.now(), false);
        emailService.createEmail(email);

        emailService.loadInbox(user);
        assertThat(cacheManager.getCache("inbox").get(user.getEmail())).isNotNull();

        emailService.moveToTrashBox(email.getEmailID());

        assertThat(cacheManager.getCache("inbox").get(user.getEmail())).isNull();
    }

    @Test
    public void testSortEmailsByDate() {
        User user = new User("test@seamail.com", "password");
        userRepository.save(user);

        LocalDateTime now = LocalDateTime.now();
        Email oldEmail = new Email("s@seamail.com", user.getEmail(), "Old", "Body", "1", now.minusDays(1), false);
        Email newEmail = new Email("s@seamail.com", user.getEmail(), "New", "Body", "1", now, false);

        emailService.createEmail(oldEmail);
        emailService.createEmail(newEmail);

        List<Email> sorted = emailService.sortEmails(user.getEmail(), "date");

        assertThat(sorted).isNotNull();
        assertThat(sorted.get(0).getSubject()).isEqualTo("Old");
    }

    @Test
    public void testFilterEmailsBySender_AlternativePath() {
        User user = new User("test@seamail.com", "password");
        userRepository.save(user);

        Email email = new Email("specific-sender@seamail.com", user.getEmail(), "Sub", "Body", "1", LocalDateTime.now(), false);
        emailService.createEmail(email);

        List<Email> filtered = emailService.filterEmails(user.getEmail(), "sender", "specific-sender@seamail.com");

        assertThat(filtered).hasSize(1);
        assertThat(filtered.get(0).getSender()).isEqualTo("specific-sender@seamail.com");
    }

    @Test
    public void testDeleteEmail_CacheEviction() {
        User user = new User("test@seamail.com", "password");
        userRepository.save(user);

        Email email = new Email("sender@seamail.com", user.getEmail(), "Subject", "Body", "1", LocalDateTime.now(), false);
        emailService.createEmail(email);

        emailService.loadInbox(user);
        assertThat(cacheManager.getCache("inbox").get(user.getEmail())).isNotNull();

        emailService.deleteEmail(email.getEmailID());
        assertThat(cacheManager.getCache("inbox").get(user.getEmail())).isNull();
    }
}