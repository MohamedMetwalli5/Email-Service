package com.backendemailservice.backendemailservice.repository;

import com.backendemailservice.backendemailservice.entity.Email;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

// Repository slice test using @DataJpaTest
@DataJpaTest
@ActiveProfiles("test")
class EmailRepositoryTest {

    @Autowired
    private EmailRepository emailRepository;

    @Test
    void shouldMoveEmailToTrashbox() {
        Email email = buildEmail("sender@seamail.com", "receiver@seamail.com", false);
        Email saved = emailRepository.save(email);

        emailRepository.moveToTrashBox(saved.getEmailID(), "receiver@seamail.com");

        Email updated = emailRepository.findById(saved.getEmailID()).orElseThrow();
        assertTrue(updated.isTrash());
    }

    @Test
    void shouldNotTrashEmailWhenReceiverDoesNotMatch() {
        Email email = buildEmail("sender@seamail.com", "receiver@seamail.com", false);
        Email saved = emailRepository.save(email);

        emailRepository.moveToTrashBox(saved.getEmailID(), "someoneElse@seamail.com");

        Email unchanged = emailRepository.findById(saved.getEmailID()).orElseThrow();
        assertFalse(unchanged.isTrash());
    }

    @Test
    void shouldLoadOnlyNonTrashedEmailsForInbox() {
        Email live = buildEmail("a@seamail.com", "b@seamail.com", false);
        Email trashed = buildEmail("c@seamail.com", "b@seamail.com", true);
        emailRepository.saveAll(List.of(live, trashed));

        Page<Email> inbox = emailRepository.loadInbox("b@seamail.com", Pageable.unpaged());

        assertEquals(1, inbox.getNumberOfElements());
        assertFalse(inbox.getContent().get(0).isTrash());
    }

    @Test
    void shouldLoadOnlySenderEmailsForOutbox() {
        Email sent1 = buildEmail("sender@seamail.com", "a@seamail.com", false);
        Email sent2 = buildEmail("sender@seamail.com", "b@seamail.com", false);
        Email other = buildEmail("other@seamail.com", "c@seamail.com", false);
        emailRepository.saveAll(List.of(sent1, sent2, other));

        Page<Email> outbox = emailRepository.loadOutbox("sender@seamail.com", Pageable.unpaged());

        assertEquals(2, outbox.getNumberOfElements());
        assertTrue(outbox.getContent().stream().allMatch(e -> e.getSender().equals("sender@seamail.com")));
    }

    @Test
    void shouldLoadOnlyTrashedEmailsForTrashbox() {
        Email live = buildEmail("a@seamail.com", "user@seamail.com", false);
        Email trashed1 = buildEmail("b@seamail.com", "user@seamail.com", true);
        Email trashed2 = buildEmail("c@seamail.com", "user@seamail.com", true);
        emailRepository.saveAll(List.of(live, trashed1, trashed2));

        Page<Email> trashbox = emailRepository.loadTrashbox("user@seamail.com", Pageable.unpaged());

        assertEquals(2, trashbox.getNumberOfElements());
        assertTrue(trashbox.getContent().stream().allMatch(Email::isTrash));
    }

    @Test
    void shouldSortEmailsByPriorityAscending() {
        Email high = buildEmail("a@b.com", "user@seamail.com", "1", false);
        Email low = buildEmail("a@b.com", "user@seamail.com", "3", false);
        Email medium = buildEmail("a@b.com", "user@seamail.com", "2", false);
        emailRepository.saveAll(List.of(high, low, medium));

        Page<Email> sorted = emailRepository.sortEmailsByPriority("user@seamail.com", Pageable.unpaged());

        assertEquals(3, sorted.getNumberOfElements());
        assertTrue(sorted.getContent().get(0).getPriority().compareTo(sorted.getContent().get(1).getPriority()) <= 0);
    }

    @Test
    void shouldSortEmailsByDateAscending() {
        LocalDateTime now = LocalDateTime.now();
        Email older = buildEmailWithDate("a@b.com", "user@seamail.com", now.minusDays(2), false);
        Email newer = buildEmailWithDate("a@b.com", "user@seamail.com", now, false);
        Email middle = buildEmailWithDate("a@b.com", "user@seamail.com", now.minusDays(1), false);
        emailRepository.saveAll(List.of(older, newer, middle));

        Page<Email> sorted = emailRepository.sortEmailsByDate("user@seamail.com", Pageable.unpaged());

        assertEquals(3, sorted.getNumberOfElements());
        assertTrue(sorted.getContent().get(0).getDate().isBefore(sorted.getContent().get(1).getDate()) ||
                   sorted.getContent().get(0).getDate().isEqual(sorted.getContent().get(1).getDate()));
    }

    @Test
    void shouldFilterEmailsBySubject() {
        Email matching = buildEmailWithSubject("a@b.com", "user@seamail.com", "Invoice", false);
        Email other = buildEmailWithSubject("a@b.com", "user@seamail.com", "Receipt", false);
        emailRepository.saveAll(List.of(matching, other));

        Page<Email> filtered = emailRepository.filterEmailsBySubject("user@seamail.com", "Invoice", Pageable.unpaged());

        assertEquals(1, filtered.getNumberOfElements());
        assertEquals("Invoice", filtered.getContent().get(0).getSubject());
    }

    @Test
    void shouldExcludeTrashedEmailsWhenFilteringInboxBySubject() {
        Email live = buildEmailWithSubject("a@b.com", "user@seamail.com", "Invoice", false);
        Email trashed = buildEmailWithSubject("a@b.com", "user@seamail.com", "Invoice", true);
        emailRepository.saveAll(List.of(live, trashed));

        Page<Email> filtered = emailRepository.filterEmailsBySubject("user@seamail.com", "Invoice", Pageable.unpaged());

        assertEquals(1, filtered.getNumberOfElements());
        assertFalse(filtered.getContent().get(0).isTrash());
    }

    @Test
    void shouldFilterEmailsBySender() {
        Email fromBoss = buildEmail("boss@b.com", "user@seamail.com", "Meeting", false);
        Email fromColleague = buildEmail("colleague@b.com", "user@seamail.com", "Hello", false);
        emailRepository.saveAll(List.of(fromBoss, fromColleague));

        Page<Email> filtered = emailRepository.filterEmailsBySender("user@seamail.com", "boss@b.com", Pageable.unpaged());

        assertEquals(1, filtered.getNumberOfElements());
        assertEquals("boss@b.com", filtered.getContent().get(0).getSender());
    }

    @Test
    void shouldExcludeTrashedEmailsWhenFilteringInboxBySender() {
        Email live = buildEmail("boss@b.com", "user@seamail.com", "Meeting", false);
        Email trashed = buildEmail("boss@b.com", "user@seamail.com", "Meeting", true);
        emailRepository.saveAll(List.of(live, trashed));

        Page<Email> filtered = emailRepository.filterEmailsBySender("user@seamail.com", "boss@b.com", Pageable.unpaged());

        assertEquals(1, filtered.getNumberOfElements());
        assertFalse(filtered.getContent().get(0).isTrash());
    }

    @Test
    void shouldSortOutboxByPriorityAscending() {
        Email high = buildEmail("user@seamail.com", "a@b.com", "1", false);
        Email low = buildEmail("user@seamail.com", "b@b.com", "3", false);
        Email medium = buildEmail("user@seamail.com", "c@b.com", "2", false);
        emailRepository.saveAll(List.of(high, low, medium));

        Page<Email> sorted = emailRepository.sortOutboxByPriority("user@seamail.com", Pageable.unpaged());

        assertEquals(3, sorted.getNumberOfElements());
        assertTrue(sorted.getContent().get(0).getPriority().compareTo(sorted.getContent().get(1).getPriority()) <= 0);
    }

    @Test
    void shouldSortOutboxByDateAscending() {
        LocalDateTime now = LocalDateTime.now();
        Email older = buildEmailWithDate("user@seamail.com", "a@b.com", now.minusDays(2), false);
        Email newer = buildEmailWithDate("user@seamail.com", "b@b.com", now, false);
        Email middle = buildEmailWithDate("user@seamail.com", "c@b.com", now.minusDays(1), false);
        emailRepository.saveAll(List.of(older, newer, middle));

        Page<Email> sorted = emailRepository.sortOutboxByDate("user@seamail.com", Pageable.unpaged());

        assertEquals(3, sorted.getNumberOfElements());
        assertTrue(sorted.getContent().get(0).getDate().isBefore(sorted.getContent().get(1).getDate()) ||
                   sorted.getContent().get(0).getDate().isEqual(sorted.getContent().get(1).getDate()));
    }

    @Test
    void shouldSortTrashboxByPriorityAscending() {
        Email high = buildEmail("a@b.com", "user@seamail.com", "1", true);
        Email low = buildEmail("b@b.com", "user@seamail.com", "3", true);
        Email medium = buildEmail("c@b.com", "user@seamail.com", "2", true);
        emailRepository.saveAll(List.of(high, low, medium));

        Page<Email> sorted = emailRepository.sortTrashboxByPriority("user@seamail.com", Pageable.unpaged());

        assertEquals(3, sorted.getNumberOfElements());
        assertTrue(sorted.getContent().get(0).getPriority().compareTo(sorted.getContent().get(1).getPriority()) <= 0);
    }

    @Test
    void shouldSortTrashboxByDateAscending() {
        LocalDateTime now = LocalDateTime.now();
        Email older = buildEmailWithDate("a@b.com", "user@seamail.com", now.minusDays(2), true);
        Email newer = buildEmailWithDate("b@b.com", "user@seamail.com", now, true);
        Email middle = buildEmailWithDate("c@b.com", "user@seamail.com", now.minusDays(1), true);
        emailRepository.saveAll(List.of(older, newer, middle));

        Page<Email> sorted = emailRepository.sortTrashboxByDate("user@seamail.com", Pageable.unpaged());

        assertEquals(3, sorted.getNumberOfElements());
        assertTrue(sorted.getContent().get(0).getDate().isBefore(sorted.getContent().get(1).getDate()) ||
                   sorted.getContent().get(0).getDate().isEqual(sorted.getContent().get(1).getDate()));
    }

    @Test
    void shouldFilterOutboxBySubject() {
        Email matching = buildEmailWithSubject("user@seamail.com", "a@b.com", "Invoice", false);
        Email other = buildEmailWithSubject("user@seamail.com", "b@b.com", "Receipt", false);
        emailRepository.saveAll(List.of(matching, other));

        Page<Email> filtered = emailRepository.filterOutboxBySubject("user@seamail.com", "Invoice", Pageable.unpaged());

        assertEquals(1, filtered.getNumberOfElements());
        assertEquals("Invoice", filtered.getContent().get(0).getSubject());
    }

    @Test
    void shouldExcludeTrashedEmailsWhenFilteringOutboxBySubject() {
        Email live = buildEmailWithSubject("user@seamail.com", "a@b.com", "Invoice", false);
        Email trashed = buildEmailWithSubject("user@seamail.com", "a@b.com", "Invoice", true);
        emailRepository.saveAll(List.of(live, trashed));

        Page<Email> filtered = emailRepository.filterOutboxBySubject("user@seamail.com", "Invoice", Pageable.unpaged());

        assertEquals(1, filtered.getNumberOfElements());
        assertFalse(filtered.getContent().get(0).isTrash());
    }

    @Test
    void shouldFilterOutboxByReceiver() {
        Email toBoss = buildEmail("user@seamail.com", "boss@b.com", "Meeting", false);
        Email toColleague = buildEmail("user@seamail.com", "colleague@b.com", "Hello", false);
        emailRepository.saveAll(List.of(toBoss, toColleague));

        Page<Email> filtered = emailRepository.filterOutboxByReceiver("user@seamail.com", "boss@b.com", Pageable.unpaged());

        assertEquals(1, filtered.getNumberOfElements());
        assertEquals("boss@b.com", filtered.getContent().get(0).getReceiver());
    }

    @Test
    void shouldExcludeTrashedEmailsWhenFilteringOutboxByReceiver() {
        Email live = buildEmail("user@seamail.com", "boss@b.com", "Meeting", false);
        Email trashed = buildEmail("user@seamail.com", "boss@b.com", "Meeting", true);
        emailRepository.saveAll(List.of(live, trashed));

        Page<Email> filtered = emailRepository.filterOutboxByReceiver("user@seamail.com", "boss@b.com", Pageable.unpaged());

        assertEquals(1, filtered.getNumberOfElements());
        assertFalse(filtered.getContent().get(0).isTrash());
    }

    @Test
    void shouldFilterTrashboxBySubject() {
        Email matching = buildEmailWithSubject("a@b.com", "user@seamail.com", "Invoice", true);
        Email other = buildEmailWithSubject("b@b.com", "user@seamail.com", "Receipt", true);
        emailRepository.saveAll(List.of(matching, other));

        Page<Email> filtered = emailRepository.filterTrashBySubject("user@seamail.com", "Invoice", Pageable.unpaged());

        assertEquals(1, filtered.getNumberOfElements());
        assertEquals("Invoice", filtered.getContent().get(0).getSubject());
    }

    @Test
    void shouldFilterTrashboxBySender() {
        Email fromBoss = buildEmail("boss@b.com", "user@seamail.com", "Meeting", true);
        Email fromColleague = buildEmail("colleague@b.com", "user@seamail.com", "Hello", true);
        emailRepository.saveAll(List.of(fromBoss, fromColleague));

        Page<Email> filtered = emailRepository.filterTrashBySender("user@seamail.com", "boss@b.com", Pageable.unpaged());

        assertEquals(1, filtered.getNumberOfElements());
        assertEquals("boss@b.com", filtered.getContent().get(0).getSender());
    }

    @Test
    void shouldReturnEmptyInboxWhenNoEmailsExist() {
        Page<Email> inbox = emailRepository.loadInbox("empty@seamail.com", Pageable.unpaged());
        assertTrue(inbox.getContent().isEmpty());
    }

    private Email buildEmail(String from, String to, boolean trash) {
        Email e = new Email();
        e.setSender(from);
        e.setReceiver(to);
        e.setSubject("Test Subject");
        e.setBody("Test Body");
        e.setPriority("low");
        e.setDate(LocalDateTime.now());
        e.setTrash(trash);
        return e;
    }

    private Email buildEmail(String from, String to, String priority, boolean trash) {
        Email e = buildEmail(from, to, trash);
        e.setPriority(priority);
        return e;
    }

    private Email buildEmailWithDate(String from, String to, LocalDateTime date, boolean trash) {
        Email e = buildEmail(from, to, trash);
        e.setDate(date);
        return e;
    }

    private Email buildEmailWithSubject(String from, String to, String subject, boolean trash) {
        Email e = buildEmail(from, to, trash);
        e.setSubject(subject);
        return e;
    }
}
