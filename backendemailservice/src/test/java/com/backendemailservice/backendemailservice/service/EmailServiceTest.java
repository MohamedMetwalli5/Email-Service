package com.backendemailservice.backendemailservice.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import com.backendemailservice.backendemailservice.FilteringWrapper;
import com.backendemailservice.backendemailservice.SortingWrapper;
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

    @BeforeEach
    public void setUp() {
        // Clearing all users and emails before each test
        emailRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    public void testCreateEmail() {
        User user = new User("test@example.com", "password");
        userRepository.save(user);

        Email email = new Email(user.getEmail(), "recipient@example.com", "Test Email", "This is a test email.", "1", "2023-10-01", "No");
        emailService.createEmail(email);

        assertThat(emailRepository.count()).isEqualTo(1);
        
        // Validating the details of the saved email
        Email savedEmail = emailRepository.findById(email.getEmailID()).orElse(null);
        assertThat(savedEmail).isNotNull();
        assertThat(savedEmail.getSender()).isEqualTo(user.getEmail());
        assertThat(savedEmail.getReceiver()).isEqualTo("recipient@example.com");
        assertThat(savedEmail.getSubject()).isEqualTo("Test Email");
        assertThat(savedEmail.getBody()).isEqualTo("This is a test email.");
        assertThat(savedEmail.getPriority()).isEqualTo("1");
        assertThat(savedEmail.getDate()).isEqualTo("2023-10-01");
        assertThat(savedEmail.getTrash()).isEqualTo("No");
    }

    @Test
    public void testLoadInbox() {
        User user = new User("test@example.com", "password");
        userRepository.save(user);
        
        Email email = new Email("sender@example.com", user.getEmail(), "Test Email", "This is a test email.", "1", "2023-10-01", "No");
        emailRepository.save(email);

        List<Email> inboxEmails = emailService.loadInbox(user);
        assertThat(inboxEmails).isNotNull();
        assertThat(inboxEmails.size()).isEqualTo(1);
        assertThat(inboxEmails.get(0).getSubject()).isEqualTo("Test Email");
    }

    @Test
    public void testLoadOutbox() {
        User user = new User("test@example.com", "password");
        userRepository.save(user);
        
        Email email = new Email(user.getEmail(), "recipient@example.com", "Test Email", "This is a test email.", "1", "2023-10-01", "No");
        emailService.createEmail(email);
        
        List<Email> outboxEmails = emailService.loadOutbox(user);
        assertThat(outboxEmails).isNotNull();
        assertThat(outboxEmails.size()).isEqualTo(1);
        assertThat(outboxEmails.get(0).getSubject()).isEqualTo("Test Email");
    }

    @Test
    public void testLoadTrashbox() {
        User user = new User("test@example.com", "password");
        userRepository.save(user);
        
        Email email = new Email("sender@example.com", user.getEmail(), "Test Email", "This is a test email.", "1", "2023-10-01", "Yes");
        emailService.createEmail(email);
        
        List<Email> trashedEmails = emailService.loadTrashbox(user);
        assertThat(trashedEmails).isNotNull();
        assertThat(trashedEmails.size()).isEqualTo(1);
        assertThat(trashedEmails.get(0).getSubject()).isEqualTo("Test Email");
    }

    @Test
    public void testDeleteEmail() {
        User user = new User("test@example.com", "password");
        userRepository.save(user);
        
        Email email = new Email(user.getEmail(), "recipient@example.com", "Test Email", "This is a test email.", "1", "2023-10-01", "No");
        emailService.createEmail(email);

        assertThat(emailRepository.count()).isEqualTo(1);

        emailService.deleteEmail(email);

        assertThat(emailRepository.count()).isEqualTo(0);
    }

    @Test
    public void testSortEmailsByPriority() {
        User user = new User("test@example.com", "password");
        userRepository.save(user);

        Email email1 = new Email("sender@example.com", user.getEmail(), "Test Email 1", "First email", "2", "2023-10-01", "No");
        Email email2 = new Email("sender@example.com", user.getEmail(), "Test Email 2", "Second email", "1", "2023-10-02", "No");
        emailService.createEmail(email1);
        emailService.createEmail(email2);

        SortingWrapper sortingWrapper = new SortingWrapper();
        sortingWrapper.setUser(user);
        sortingWrapper.setSortingOption("priority");

        List<Email> userEmails = emailRepository.loadInbox(user.getEmail());
        List<Email> sortedEmails = emailRepository.sortEmailsByPriority(user.getEmail());

        assertThat(sortedEmails).isNotNull();
        assertThat(sortedEmails.size()).isEqualTo(userEmails.size());
    }

    @Test
    public void testFilterEmailsBySubject() {
        User user = new User("test@example.com", "password");
        userRepository.save(user);

        Email email1 = new Email("sender@example.com", user.getEmail(), "Important Test Email", "First email", "1", "2023-10-01", "No");
        Email email2 = new Email("sender@example.com", user.getEmail(), "Another Test Email", "Second email", "1", "2023-10-02", "No");
        emailService.createEmail(email1);
        emailService.createEmail(email2);

        FilteringWrapper filteringWrapper = new FilteringWrapper();
        filteringWrapper.setUser(user);
        filteringWrapper.setFilteringOption("subject");
        filteringWrapper.setFilteringValue("Important Test Email");

        List<Email> filteredEmails = emailService.filterEmails(filteringWrapper);

        assertThat(filteredEmails).isNotNull();
        assertThat(filteredEmails.size()).isEqualTo(1);
        assertThat(filteredEmails.get(0).getSubject()).isEqualTo("Important Test Email");
    }

    @Test
    public void testFilterEmailsBySender() {
        User user = new User("test@example.com", "password");
        userRepository.save(user);
        
        Email email1 = new Email("sender1@example.com", user.getEmail(), "Test Email 1", "First email", "1", "2023-10-01", "No");
        Email email2 = new Email("sender2@example.com", user.getEmail(), "Test Email 2", "Second email", "1", "2023-10-02", "No");
        emailService.createEmail(email1);
        emailService.createEmail(email2);
        
        FilteringWrapper filteringWrapper = new FilteringWrapper();
        filteringWrapper.setUser(user);
        filteringWrapper.setFilteringOption("sender");
        filteringWrapper.setFilteringValue("sender1@example.com");

        List<Email> filteredEmails = emailService.filterEmails(filteringWrapper);
        
        assertThat(filteredEmails).isNotNull();
        assertThat(filteredEmails.size()).isEqualTo(1);
        assertThat(filteredEmails.get(0).getSender()).isEqualTo("sender1@example.com");
    }

    @Test
    public void testMoveToTrashBox() {
        User user = new User("test@example.com", "password");
        userRepository.save(user);
        
        Email email = new Email("sender@example.com", user.getEmail(), "Test Email", "This is a test email.", "1", "2023-10-01", "No");
        emailService.createEmail(email);
        
        emailService.moveToTrashBox(email);
        
        Email trashedEmail = emailRepository.findById(email.getEmailID()).orElse(null);
        assertThat(trashedEmail).isNotNull();
        assertThat(trashedEmail.getTrash()).isEqualTo("Yes"); // Checking for the updated trash value
    }
}