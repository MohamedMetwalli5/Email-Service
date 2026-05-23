package com.backendemailservice.backendemailservice.service;

import com.backendemailservice.backendemailservice.dto.EmailResponseDto;
import com.backendemailservice.backendemailservice.dto.SendEmailRequestDto;
import com.backendemailservice.backendemailservice.entity.Email;
import com.backendemailservice.backendemailservice.entity.User;
import com.backendemailservice.backendemailservice.exception.EmailNotFoundException;
import com.backendemailservice.backendemailservice.exception.ReceiverNotFoundException;
import com.backendemailservice.backendemailservice.repository.EmailRepository;
import com.backendemailservice.backendemailservice.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.CacheManager;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

// Pure unit test using MockitoExtension instead of @SpringBootTest
@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @Mock
    private EmailRepository emailRepository;

    @Mock
    private CacheManager cacheManager;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private EmailService emailService;

    // --- loadInboxDtos ---

    @Test
    void shouldLoadInboxDtos() {
        String userEmail = "user@seamail.com";
        Email entity = new Email("sender@seamail.com", userEmail, "Subject", "Body", "1",
                LocalDateTime.now(), false);

        when(emailRepository.loadInbox(userEmail)).thenReturn(List.of(entity));

        List<EmailResponseDto> result = emailService.loadInboxDtos(userEmail);

        assertEquals(1, result.size());
        assertEquals("Subject", result.get(0).getSubject());
        assertEquals(userEmail, result.get(0).getReceiver());
        verify(emailRepository).loadInbox(userEmail);
    }

    // --- loadOutboxDtos ---

    @Test
    void shouldLoadOutboxDtos() {
        String userEmail = "sender@seamail.com";
        Email entity = new Email(userEmail, "receiver@seamail.com", "Out", "Body", "1",
                LocalDateTime.now(), false);

        when(emailRepository.loadOutbox(userEmail)).thenReturn(List.of(entity));

        List<EmailResponseDto> result = emailService.loadOutboxDtos(userEmail);

        assertEquals(1, result.size());
        assertEquals("Out", result.get(0).getSubject());
        verify(emailRepository).loadOutbox(userEmail);
    }

    // --- loadTrashboxDtos ---

    @Test
    void shouldLoadTrashboxDtos() {
        String userEmail = "user@seamail.com";
        Email entity = new Email("sender@seamail.com", userEmail, "Trashed", "Body", "1",
                LocalDateTime.now(), true);

        when(emailRepository.loadTrashbox(userEmail)).thenReturn(List.of(entity));

        List<EmailResponseDto> result = emailService.loadTrashboxDtos(userEmail);

        assertEquals(1, result.size());
        assertTrue(result.get(0).isTrash());
        verify(emailRepository).loadTrashbox(userEmail);
    }

    // --- sendEmail ---

    @Test
    void shouldSendEmailWhenReceiverExists() {
        String senderEmail = "sender@seamail.com";
        String receiverEmail = "receiver@seamail.com";

        SendEmailRequestDto request = new SendEmailRequestDto();
        request.setReceiver(receiverEmail);
        request.setSubject("Test Subject");
        request.setBody("Test Body");
        request.setPriority("1");

        when(userRepository.findByEmail(receiverEmail))
                .thenReturn(Optional.of(new User(receiverEmail, "pass")));

        emailService.sendEmail(senderEmail, request);

        ArgumentCaptor<Email> captor = ArgumentCaptor.forClass(Email.class);
        verify(emailRepository).save(captor.capture());

        Email saved = captor.getValue();
        assertEquals(senderEmail, saved.getSender());
        assertEquals(receiverEmail, saved.getReceiver());
        assertEquals("Test Subject", saved.getSubject());
        assertEquals("Test Body", saved.getBody());
        assertEquals("1", saved.getPriority());
        assertFalse(saved.isTrash());
        assertNotNull(saved.getDate());
    }

    @Test
    void shouldThrowReceiverNotFoundWhenReceiverDoesNotExist() {
        SendEmailRequestDto request = new SendEmailRequestDto();
        request.setReceiver("missing@seamail.com");

        when(userRepository.findByEmail("missing@seamail.com")).thenReturn(Optional.empty());

        ReceiverNotFoundException ex = assertThrows(ReceiverNotFoundException.class,
                () -> emailService.sendEmail("sender@seamail.com", request));

        assertEquals("RECEIVER_NOT_FOUND", ex.getErrorCode());
        verifyNoInteractions(emailRepository);
    }

    // --- deleteEmail (auth-check overload) ---

    @Test
    void shouldDeleteEmailWhenEmailExists() {
        Long emailId = 1L;
        when(emailRepository.findById(emailId)).thenReturn(Optional.of(new Email()));

        emailService.deleteEmail(emailId, "user@seamail.com");

        verify(emailRepository).findById(emailId);
        verify(emailRepository).deleteById(emailId);
    }

    @Test
    void shouldThrowEmailNotFoundWhenDeletingNonExistentEmail() {
        Long emailId = 999L;
        when(emailRepository.findById(emailId)).thenReturn(Optional.empty());

        EmailNotFoundException ex = assertThrows(EmailNotFoundException.class,
                () -> emailService.deleteEmail(emailId, "user@seamail.com"));

        assertTrue(ex.getMessage().contains("999"));
        verify(emailRepository, never()).deleteById(any());
    }

    // --- moveToTrashBox (auth-check overload) ---

    @Test
    void shouldMoveEmailToTrashWhenEmailExists() {
        Long emailId = 1L;
        when(emailRepository.findById(emailId)).thenReturn(Optional.of(new Email()));

        emailService.moveToTrashBox(emailId, "user@seamail.com");

        verify(emailRepository).findById(emailId);
        verify(emailRepository).moveToTrashBox(emailId);
    }

    @Test
    void shouldThrowEmailNotFoundWhenMovingNonExistentEmailToTrash() {
        Long emailId = 999L;
        when(emailRepository.findById(emailId)).thenReturn(Optional.empty());

        EmailNotFoundException ex = assertThrows(EmailNotFoundException.class,
                () -> emailService.moveToTrashBox(emailId, "user@seamail.com"));

        assertTrue(ex.getMessage().contains("999"));
        verify(emailRepository, never()).moveToTrashBox(any());
    }

    // --- queryEmails ---

    @Test
    void shouldQueryEmailsByPrioritySort() {
        String userEmail = "user@seamail.com";
        Email e1 = new Email("a@b.com", userEmail, "Low", "B", "1", LocalDateTime.now(), false);

        when(emailRepository.sortEmailsByPriority(userEmail)).thenReturn(List.of(e1));

        List<EmailResponseDto> result = emailService.queryEmails(userEmail, "priority", null, null, null);

        assertEquals(1, result.size());
        assertEquals("Low", result.get(0).getSubject());
        verify(emailRepository).sortEmailsByPriority(userEmail);
    }

    @Test
    void shouldQueryEmailsByDateSort() {
        String userEmail = "user@seamail.com";
        Email old = new Email("a@b.com", userEmail, "Old", "B", "1",
                LocalDateTime.now().minusDays(2), false);

        when(emailRepository.sortEmailsByDate(userEmail)).thenReturn(List.of(old));

        List<EmailResponseDto> result = emailService.queryEmails(userEmail, "date", null, null, null);

        assertEquals(1, result.size());
        assertEquals("Old", result.get(0).getSubject());
        verify(emailRepository).sortEmailsByDate(userEmail);
    }

    @Test
    void shouldFilterEmailsBySubject() {
        String userEmail = "user@seamail.com";
        Email match = new Email("boss@b.com", userEmail, "Invoice", "B", "1",
                LocalDateTime.now(), false);

        when(emailRepository.filterEmailsBySubject(userEmail, "Invoice")).thenReturn(List.of(match));

        List<EmailResponseDto> result = emailService.queryEmails(userEmail, null, "subject", "Invoice", null);

        assertEquals(1, result.size());
        assertEquals("Invoice", result.get(0).getSubject());
        verify(emailRepository).filterEmailsBySubject(userEmail, "Invoice");
    }

    @Test
    void shouldFilterEmailsBySender() {
        String userEmail = "user@seamail.com";
        Email match = new Email("specific@b.com", userEmail, "Sub", "B", "1",
                LocalDateTime.now(), false);

        when(emailRepository.filterEmailsBySender(userEmail, "specific@b.com")).thenReturn(List.of(match));

        List<EmailResponseDto> result = emailService.queryEmails(userEmail, null, "sender", "specific@b.com", null);

        assertEquals(1, result.size());
        assertEquals("specific@b.com", result.get(0).getSender());
        verify(emailRepository).filterEmailsBySender(userEmail, "specific@b.com");
    }

    @Test
    void shouldDefaultToInboxWhenNoSortOrFilterProvided() {
        String userEmail = "user@seamail.com";
        Email entity = new Email("sender@seamail.com", userEmail, "Default", "B", "1",
                LocalDateTime.now(), false);

        when(emailRepository.loadInbox(userEmail)).thenReturn(List.of(entity));

        List<EmailResponseDto> result = emailService.queryEmails(userEmail, null, null, null, null);

        assertEquals(1, result.size());
        assertEquals("Default", result.get(0).getSubject());
        verify(emailRepository).loadInbox(userEmail);
    }

    // --- queryEmails with Outbox ---

    @Test
    void shouldQueryOutboxByPrioritySort() {
        String userEmail = "sender@seamail.com";
        Email e1 = new Email(userEmail, "a@b.com", "Out", "B", "1", LocalDateTime.now(), false);

        when(emailRepository.sortOutboxByPriority(userEmail)).thenReturn(List.of(e1));

        List<EmailResponseDto> result = emailService.queryEmails(userEmail, "priority", null, null, "Outbox");

        assertEquals(1, result.size());
        assertEquals("Out", result.get(0).getSubject());
        verify(emailRepository).sortOutboxByPriority(userEmail);
    }

    @Test
    void shouldQueryOutboxByDateSort() {
        String userEmail = "sender@seamail.com";
        Email old = new Email(userEmail, "a@b.com", "Old", "B", "1",
                LocalDateTime.now().minusDays(2), false);

        when(emailRepository.sortOutboxByDate(userEmail)).thenReturn(List.of(old));

        List<EmailResponseDto> result = emailService.queryEmails(userEmail, "date", null, null, "Outbox");

        assertEquals(1, result.size());
        assertEquals("Old", result.get(0).getSubject());
        verify(emailRepository).sortOutboxByDate(userEmail);
    }

    @Test
    void shouldFilterOutboxBySubject() {
        String userEmail = "sender@seamail.com";
        Email match = new Email(userEmail, "a@b.com", "Invoice", "B", "1",
                LocalDateTime.now(), false);

        when(emailRepository.filterOutboxBySubject(userEmail, "Invoice")).thenReturn(List.of(match));

        List<EmailResponseDto> result = emailService.queryEmails(userEmail, null, "subject", "Invoice", "Outbox");

        assertEquals(1, result.size());
        assertEquals("Invoice", result.get(0).getSubject());
        verify(emailRepository).filterOutboxBySubject(userEmail, "Invoice");
    }

    @Test
    void shouldFilterOutboxByReceiver() {
        String userEmail = "sender@seamail.com";
        Email match = new Email(userEmail, "specific@b.com", "Sub", "B", "1",
                LocalDateTime.now(), false);

        when(emailRepository.filterOutboxByReceiver(userEmail, "specific@b.com")).thenReturn(List.of(match));

        List<EmailResponseDto> result = emailService.queryEmails(userEmail, null, "sender", "specific@b.com", "Outbox");

        assertEquals(1, result.size());
        assertEquals("specific@b.com", result.get(0).getReceiver());
        verify(emailRepository).filterOutboxByReceiver(userEmail, "specific@b.com");
    }

    @Test
    void shouldDefaultToOutboxWhenNoSortOrFilterProvided() {
        String userEmail = "sender@seamail.com";
        Email entity = new Email(userEmail, "a@b.com", "Default", "B", "1",
                LocalDateTime.now(), false);

        when(emailRepository.loadOutbox(userEmail)).thenReturn(List.of(entity));

        List<EmailResponseDto> result = emailService.queryEmails(userEmail, null, null, null, "Outbox");

        assertEquals(1, result.size());
        assertEquals("Default", result.get(0).getSubject());
        verify(emailRepository).loadOutbox(userEmail);
    }

    // --- queryEmails with Trashbox ---

    @Test
    void shouldQueryTrashboxByPrioritySort() {
        String userEmail = "user@seamail.com";
        Email e1 = new Email("a@b.com", userEmail, "Trashed", "B", "1", LocalDateTime.now(), true);

        when(emailRepository.sortTrashboxByPriority(userEmail)).thenReturn(List.of(e1));

        List<EmailResponseDto> result = emailService.queryEmails(userEmail, "priority", null, null, "Trashbox");

        assertEquals(1, result.size());
        assertEquals("Trashed", result.get(0).getSubject());
        verify(emailRepository).sortTrashboxByPriority(userEmail);
    }

    @Test
    void shouldQueryTrashboxByDateSort() {
        String userEmail = "user@seamail.com";
        Email old = new Email("a@b.com", userEmail, "Old", "B", "1",
                LocalDateTime.now().minusDays(2), true);

        when(emailRepository.sortTrashboxByDate(userEmail)).thenReturn(List.of(old));

        List<EmailResponseDto> result = emailService.queryEmails(userEmail, "date", null, null, "Trashbox");

        assertEquals(1, result.size());
        assertEquals("Old", result.get(0).getSubject());
        verify(emailRepository).sortTrashboxByDate(userEmail);
    }

    @Test
    void shouldFilterTrashboxBySubject() {
        String userEmail = "user@seamail.com";
        Email match = new Email("boss@b.com", userEmail, "Invoice", "B", "1",
                LocalDateTime.now(), true);

        when(emailRepository.filterTrashBySubject(userEmail, "Invoice")).thenReturn(List.of(match));

        List<EmailResponseDto> result = emailService.queryEmails(userEmail, null, "subject", "Invoice", "Trashbox");

        assertEquals(1, result.size());
        assertEquals("Invoice", result.get(0).getSubject());
        verify(emailRepository).filterTrashBySubject(userEmail, "Invoice");
    }

    @Test
    void shouldFilterTrashboxBySender() {
        String userEmail = "user@seamail.com";
        Email match = new Email("boss@b.com", userEmail, "Sub", "B", "1",
                LocalDateTime.now(), true);

        when(emailRepository.filterTrashBySender(userEmail, "boss@b.com")).thenReturn(List.of(match));

        List<EmailResponseDto> result = emailService.queryEmails(userEmail, null, "sender", "boss@b.com", "Trashbox");

        assertEquals(1, result.size());
        assertEquals("boss@b.com", result.get(0).getSender());
        verify(emailRepository).filterTrashBySender(userEmail, "boss@b.com");
    }

    @Test
    void shouldDefaultToTrashboxWhenNoSortOrFilterProvided() {
        String userEmail = "user@seamail.com";
        Email entity = new Email("sender@seamail.com", userEmail, "Default", "B", "1",
                LocalDateTime.now(), true);

        when(emailRepository.loadTrashbox(userEmail)).thenReturn(List.of(entity));

        List<EmailResponseDto> result = emailService.queryEmails(userEmail, null, null, null, "Trashbox");

        assertEquals(1, result.size());
        assertEquals("Default", result.get(0).getSubject());
        verify(emailRepository).loadTrashbox(userEmail);
    }

    // --- Combined filter + sort ---

    @Test
    void shouldFilterBySubjectAndSortByPriority() {
        String userEmail = "user@seamail.com";
        Email low = new Email("a@b.com", userEmail, "Invoice", "B", "3",
                LocalDateTime.now(), false);
        Email high = new Email("c@b.com", userEmail, "Invoice", "B", "1",
                LocalDateTime.now(), false);

        when(emailRepository.filterEmailsBySubject(userEmail, "Invoice"))
                .thenReturn(new ArrayList<>(List.of(low, high)));

        List<EmailResponseDto> result = emailService.queryEmails(userEmail, "priority", "subject", "Invoice", null);

        assertEquals(2, result.size());
        assertEquals("1", result.get(0).getPriority());
        assertEquals("3", result.get(1).getPriority());
        verify(emailRepository).filterEmailsBySubject(userEmail, "Invoice");
    }

    @Test
    void shouldFilterOutboxByReceiverAndSortByDate() {
        String userEmail = "sender@seamail.com";
        LocalDateTime now = LocalDateTime.now();
        Email older = new Email(userEmail, "specific@b.com", "Old", "B", "1",
                now.minusDays(2), false);
        Email newer = new Email(userEmail, "specific@b.com", "New", "B", "1",
                now, false);

        when(emailRepository.filterOutboxByReceiver(userEmail, "specific@b.com"))
                .thenReturn(new ArrayList<>(List.of(newer, older)));

        List<EmailResponseDto> result = emailService.queryEmails(userEmail, "date", "sender", "specific@b.com", "Outbox");

        assertEquals(2, result.size());
        assertTrue(result.get(0).getDate().isBefore(result.get(1).getDate()) ||
                   result.get(0).getDate().isEqual(result.get(1).getDate()));
        verify(emailRepository).filterOutboxByReceiver(userEmail, "specific@b.com");
    }

    @Test
    void shouldFilterTrashboxBySubjectAndSortByPriority() {
        String userEmail = "user@seamail.com";
        Email low = new Email("a@b.com", userEmail, "Invoice", "B", "3",
                LocalDateTime.now(), true);
        Email high = new Email("c@b.com", userEmail, "Invoice", "B", "1",
                LocalDateTime.now(), true);

        when(emailRepository.filterTrashBySubject(userEmail, "Invoice"))
                .thenReturn(new ArrayList<>(List.of(low, high)));

        List<EmailResponseDto> result = emailService.queryEmails(userEmail, "priority", "subject", "Invoice", "Trashbox");

        assertEquals(2, result.size());
        assertEquals("1", result.get(0).getPriority());
        assertEquals("3", result.get(1).getPriority());
        verify(emailRepository).filterTrashBySubject(userEmail, "Invoice");
    }
}
