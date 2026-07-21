package com.seamail.mail.service;

import com.seamail.mail.dto.EmailResponseDto;
import com.seamail.mail.dto.SendEmailRequestDto;
import com.seamail.mail.entity.Email;
import com.seamail.mail.entity.User;
import com.seamail.mail.exception.EmailNotFoundException;
import com.seamail.mail.exception.ReceiverNotFoundException;
import com.seamail.mail.repository.EmailRepository;
import com.seamail.mail.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

// Pure unit test using MockitoExtension instead of @SpringBootTest
@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @Mock
    private EmailRepository emailRepository;

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

        when(emailRepository.loadInbox(eq(userEmail), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(entity)));

        Page<EmailResponseDto> result = emailService.loadInboxDtos(userEmail, Pageable.unpaged());

        assertEquals(1, result.getContent().size());
        assertEquals("Subject", result.getContent().get(0).getSubject());
        assertEquals(userEmail, result.getContent().get(0).getReceiver());
        verify(emailRepository).loadInbox(eq(userEmail), any(Pageable.class));
    }

    // --- loadOutboxDtos ---

    @Test
    void shouldLoadOutboxDtos() {
        String userEmail = "sender@seamail.com";
        Email entity = new Email(userEmail, "receiver@seamail.com", "Out", "Body", "1",
                LocalDateTime.now(), false);

        when(emailRepository.loadOutbox(eq(userEmail), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(entity)));

        Page<EmailResponseDto> result = emailService.loadOutboxDtos(userEmail, Pageable.unpaged());

        assertEquals(1, result.getContent().size());
        assertEquals("Out", result.getContent().get(0).getSubject());
        verify(emailRepository).loadOutbox(eq(userEmail), any(Pageable.class));
    }

    // --- loadTrashboxDtos ---

    @Test
    void shouldLoadTrashboxDtos() {
        String userEmail = "user@seamail.com";
        Email entity = new Email("sender@seamail.com", userEmail, "Trashed", "Body", "1",
                LocalDateTime.now(), true);

        when(emailRepository.loadTrashbox(eq(userEmail), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(entity)));

        Page<EmailResponseDto> result = emailService.loadTrashboxDtos(userEmail, Pageable.unpaged());

        assertEquals(1, result.getContent().size());
        assertTrue(result.getContent().get(0).isTrash());
        verify(emailRepository).loadTrashbox(eq(userEmail), any(Pageable.class));
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
    void shouldDeleteEmailWhenEmailIsTrashed() {
        Long emailId = 1L;
        Email owned = new Email("sender@seamail.com", "user@seamail.com",
                "Sub", "Body", "1", LocalDateTime.now(), true);
        when(emailRepository.findById(emailId)).thenReturn(Optional.of(owned));

        emailService.deleteEmail(emailId, "user@seamail.com");

        verify(emailRepository).findById(emailId);
        verify(emailRepository).deleteById(emailId);
    }

    @Test
    void shouldThrowWhenDeletingEmailThatIsNotTrashed() {
        Long emailId = 1L;
        Email owned = new Email("sender@seamail.com", "user@seamail.com",
                "Sub", "Body", "1", LocalDateTime.now(), false);
        when(emailRepository.findById(emailId)).thenReturn(Optional.of(owned));

        assertThrows(org.springframework.web.server.ResponseStatusException.class,
                () -> emailService.deleteEmail(emailId, "user@seamail.com"));

        verify(emailRepository, never()).deleteById(any());
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

    @Test
    void shouldThrowWhenDeletingEmailOwnedByAnotherUser() {
        Long emailId = 1L;
        Email foreignEmail = new Email("sender@seamail.com", "other@seamail.com",
                "Sub", "Body", "1", LocalDateTime.now(), false);
        when(emailRepository.findById(emailId)).thenReturn(Optional.of(foreignEmail));

        assertThrows(org.springframework.web.server.ResponseStatusException.class,
                () -> emailService.deleteEmail(emailId, "user@seamail.com"));

        verify(emailRepository, never()).deleteById(any());
    }

    // --- moveToTrashBox (auth-check overload) ---

    @Test
    void shouldMoveEmailToTrashWhenEmailExists() {
        Long emailId = 1L;
        Email owned = new Email("sender@seamail.com", "user@seamail.com",
                "Sub", "Body", "1", LocalDateTime.now(), false);
        when(emailRepository.findById(emailId)).thenReturn(Optional.of(owned));

        emailService.moveToTrashBox(emailId, "user@seamail.com");

        verify(emailRepository).findById(emailId);
        verify(emailRepository).moveToTrashBox(emailId, "user@seamail.com");
    }

    @Test
    void shouldThrowEmailNotFoundWhenMovingNonExistentEmailToTrash() {
        Long emailId = 999L;
        when(emailRepository.findById(emailId)).thenReturn(Optional.empty());

        EmailNotFoundException ex = assertThrows(EmailNotFoundException.class,
                () -> emailService.moveToTrashBox(emailId, "user@seamail.com"));

        assertTrue(ex.getMessage().contains("999"));
        verify(emailRepository, never()).moveToTrashBox(any(), any());
    }

    @Test
    void shouldThrowWhenMovingEmailOwnedByAnotherUserToTrash() {
        Long emailId = 1L;
        Email foreignEmail = new Email("sender@seamail.com", "other@seamail.com",
                "Sub", "Body", "1", LocalDateTime.now(), false);
        when(emailRepository.findById(emailId)).thenReturn(Optional.of(foreignEmail));

        assertThrows(org.springframework.web.server.ResponseStatusException.class,
                () -> emailService.moveToTrashBox(emailId, "user@seamail.com"));

        verify(emailRepository, never()).moveToTrashBox(any(), any());
    }

    // --- queryEmails ---

    @Test
    void shouldQueryEmailsByPrioritySort() {
        String userEmail = "user@seamail.com";
        Email e1 = new Email("a@b.com", userEmail, "Low", "B", "1", LocalDateTime.now(), false);

        when(emailRepository.sortEmailsByPriority(eq(userEmail), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(e1)));

        Page<EmailResponseDto> result = emailService.queryEmails(userEmail, "priority", null, null, null, Pageable.unpaged());

        assertEquals(1, result.getContent().size());
        assertEquals("Low", result.getContent().get(0).getSubject());
        verify(emailRepository).sortEmailsByPriority(eq(userEmail), any(Pageable.class));
    }

    @Test
    void shouldQueryEmailsByDateSort() {
        String userEmail = "user@seamail.com";
        Email old = new Email("a@b.com", userEmail, "Old", "B", "1",
                LocalDateTime.now().minusDays(2), false);

        when(emailRepository.sortEmailsByDate(eq(userEmail), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(old)));

        Page<EmailResponseDto> result = emailService.queryEmails(userEmail, "date", null, null, null, Pageable.unpaged());

        assertEquals(1, result.getContent().size());
        assertEquals("Old", result.getContent().get(0).getSubject());
        verify(emailRepository).sortEmailsByDate(eq(userEmail), any(Pageable.class));
    }

    @Test
    void shouldFilterEmailsBySubject() {
        String userEmail = "user@seamail.com";
        Email match = new Email("boss@b.com", userEmail, "Invoice", "B", "1",
                LocalDateTime.now(), false);

        when(emailRepository.filterEmailsBySubject(eq(userEmail), eq("Invoice"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(match)));

        Page<EmailResponseDto> result = emailService.queryEmails(userEmail, null, "subject", "Invoice", null, Pageable.unpaged());

        assertEquals(1, result.getContent().size());
        assertEquals("Invoice", result.getContent().get(0).getSubject());
        verify(emailRepository).filterEmailsBySubject(eq(userEmail), eq("Invoice"), any(Pageable.class));
    }

    @Test
    void shouldFilterEmailsBySender() {
        String userEmail = "user@seamail.com";
        Email match = new Email("specific@b.com", userEmail, "Sub", "B", "1",
                LocalDateTime.now(), false);

        when(emailRepository.filterEmailsBySender(eq(userEmail), eq("specific@b.com"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(match)));

        Page<EmailResponseDto> result = emailService.queryEmails(userEmail, null, "sender", "specific@b.com", null, Pageable.unpaged());

        assertEquals(1, result.getContent().size());
        assertEquals("specific@b.com", result.getContent().get(0).getSender());
        verify(emailRepository).filterEmailsBySender(eq(userEmail), eq("specific@b.com"), any(Pageable.class));
    }

    @Test
    void shouldDefaultToInboxWhenNoSortOrFilterProvided() {
        String userEmail = "user@seamail.com";
        Email entity = new Email("sender@seamail.com", userEmail, "Default", "B", "1",
                LocalDateTime.now(), false);

        when(emailRepository.loadInbox(eq(userEmail), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(entity)));

        Page<EmailResponseDto> result = emailService.queryEmails(userEmail, null, null, null, null, Pageable.unpaged());

        assertEquals(1, result.getContent().size());
        assertEquals("Default", result.getContent().get(0).getSubject());
        verify(emailRepository).loadInbox(eq(userEmail), any(Pageable.class));
    }

    // --- queryEmails with Outbox ---

    @Test
    void shouldQueryOutboxByPrioritySort() {
        String userEmail = "sender@seamail.com";
        Email e1 = new Email(userEmail, "a@b.com", "Out", "B", "1", LocalDateTime.now(), false);

        when(emailRepository.sortOutboxByPriority(eq(userEmail), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(e1)));

        Page<EmailResponseDto> result = emailService.queryEmails(userEmail, "priority", null, null, "Outbox", Pageable.unpaged());

        assertEquals(1, result.getContent().size());
        assertEquals("Out", result.getContent().get(0).getSubject());
        verify(emailRepository).sortOutboxByPriority(eq(userEmail), any(Pageable.class));
    }

    @Test
    void shouldQueryOutboxByDateSort() {
        String userEmail = "sender@seamail.com";
        Email old = new Email(userEmail, "a@b.com", "Old", "B", "1",
                LocalDateTime.now().minusDays(2), false);

        when(emailRepository.sortOutboxByDate(eq(userEmail), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(old)));

        Page<EmailResponseDto> result = emailService.queryEmails(userEmail, "date", null, null, "Outbox", Pageable.unpaged());

        assertEquals(1, result.getContent().size());
        assertEquals("Old", result.getContent().get(0).getSubject());
        verify(emailRepository).sortOutboxByDate(eq(userEmail), any(Pageable.class));
    }

    @Test
    void shouldFilterOutboxBySubject() {
        String userEmail = "sender@seamail.com";
        Email match = new Email(userEmail, "a@b.com", "Invoice", "B", "1",
                LocalDateTime.now(), false);

        when(emailRepository.filterOutboxBySubject(eq(userEmail), eq("Invoice"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(match)));

        Page<EmailResponseDto> result = emailService.queryEmails(userEmail, null, "subject", "Invoice", "Outbox", Pageable.unpaged());

        assertEquals(1, result.getContent().size());
        assertEquals("Invoice", result.getContent().get(0).getSubject());
        verify(emailRepository).filterOutboxBySubject(eq(userEmail), eq("Invoice"), any(Pageable.class));
    }

    @Test
    void shouldFilterOutboxByReceiver() {
        String userEmail = "sender@seamail.com";
        Email match = new Email(userEmail, "specific@b.com", "Sub", "B", "1",
                LocalDateTime.now(), false);

        when(emailRepository.filterOutboxByReceiver(eq(userEmail), eq("specific@b.com"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(match)));

        Page<EmailResponseDto> result = emailService.queryEmails(userEmail, null, "sender", "specific@b.com", "Outbox", Pageable.unpaged());

        assertEquals(1, result.getContent().size());
        assertEquals("specific@b.com", result.getContent().get(0).getReceiver());
        verify(emailRepository).filterOutboxByReceiver(eq(userEmail), eq("specific@b.com"), any(Pageable.class));
    }

    @Test
    void shouldDefaultToOutboxWhenNoSortOrFilterProvided() {
        String userEmail = "sender@seamail.com";
        Email entity = new Email(userEmail, "a@b.com", "Default", "B", "1",
                LocalDateTime.now(), false);

        when(emailRepository.loadOutbox(eq(userEmail), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(entity)));

        Page<EmailResponseDto> result = emailService.queryEmails(userEmail, null, null, null, "Outbox", Pageable.unpaged());

        assertEquals(1, result.getContent().size());
        assertEquals("Default", result.getContent().get(0).getSubject());
        verify(emailRepository).loadOutbox(eq(userEmail), any(Pageable.class));
    }

    // --- queryEmails with Trashbox ---

    @Test
    void shouldQueryTrashboxByPrioritySort() {
        String userEmail = "user@seamail.com";
        Email e1 = new Email("a@b.com", userEmail, "Trashed", "B", "1", LocalDateTime.now(), true);

        when(emailRepository.sortTrashboxByPriority(eq(userEmail), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(e1)));

        Page<EmailResponseDto> result = emailService.queryEmails(userEmail, "priority", null, null, "Trashbox", Pageable.unpaged());

        assertEquals(1, result.getContent().size());
        assertEquals("Trashed", result.getContent().get(0).getSubject());
        verify(emailRepository).sortTrashboxByPriority(eq(userEmail), any(Pageable.class));
    }

    @Test
    void shouldQueryTrashboxByDateSort() {
        String userEmail = "user@seamail.com";
        Email old = new Email("a@b.com", userEmail, "Old", "B", "1",
                LocalDateTime.now().minusDays(2), true);

        when(emailRepository.sortTrashboxByDate(eq(userEmail), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(old)));

        Page<EmailResponseDto> result = emailService.queryEmails(userEmail, "date", null, null, "Trashbox", Pageable.unpaged());

        assertEquals(1, result.getContent().size());
        assertEquals("Old", result.getContent().get(0).getSubject());
        verify(emailRepository).sortTrashboxByDate(eq(userEmail), any(Pageable.class));
    }

    @Test
    void shouldFilterTrashboxBySubject() {
        String userEmail = "user@seamail.com";
        Email match = new Email("boss@b.com", userEmail, "Invoice", "B", "1",
                LocalDateTime.now(), true);

        when(emailRepository.filterTrashBySubject(eq(userEmail), eq("Invoice"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(match)));

        Page<EmailResponseDto> result = emailService.queryEmails(userEmail, null, "subject", "Invoice", "Trashbox", Pageable.unpaged());

        assertEquals(1, result.getContent().size());
        assertEquals("Invoice", result.getContent().get(0).getSubject());
        verify(emailRepository).filterTrashBySubject(eq(userEmail), eq("Invoice"), any(Pageable.class));
    }

    @Test
    void shouldFilterTrashboxBySender() {
        String userEmail = "user@seamail.com";
        Email match = new Email("boss@b.com", userEmail, "Sub", "B", "1",
                LocalDateTime.now(), true);

        when(emailRepository.filterTrashBySender(eq(userEmail), eq("boss@b.com"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(match)));

        Page<EmailResponseDto> result = emailService.queryEmails(userEmail, null, "sender", "boss@b.com", "Trashbox", Pageable.unpaged());

        assertEquals(1, result.getContent().size());
        assertEquals("boss@b.com", result.getContent().get(0).getSender());
        verify(emailRepository).filterTrashBySender(eq(userEmail), eq("boss@b.com"), any(Pageable.class));
    }

    @Test
    void shouldDefaultToTrashboxWhenNoSortOrFilterProvided() {
        String userEmail = "user@seamail.com";
        Email entity = new Email("sender@seamail.com", userEmail, "Default", "B", "1",
                LocalDateTime.now(), true);

        when(emailRepository.loadTrashbox(eq(userEmail), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(entity)));

        Page<EmailResponseDto> result = emailService.queryEmails(userEmail, null, null, null, "Trashbox", Pageable.unpaged());

        assertEquals(1, result.getContent().size());
        assertEquals("Default", result.getContent().get(0).getSubject());
        verify(emailRepository).loadTrashbox(eq(userEmail), any(Pageable.class));
    }

    // --- Combined filter + sort ---

    @Test
    void shouldFilterBySubjectAndSortByPriority() {
        String userEmail = "user@seamail.com";
        Email low = new Email("a@b.com", userEmail, "Invoice", "B", "3",
                LocalDateTime.now(), false);
        Email high = new Email("c@b.com", userEmail, "Invoice", "B", "1",
                LocalDateTime.now(), false);

        when(emailRepository.filterEmailsBySubject(eq(userEmail), eq("Invoice"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(new ArrayList<>(List.of(low, high))));

        Page<EmailResponseDto> result = emailService.queryEmails(userEmail, "priority", "subject", "Invoice", null, Pageable.unpaged());

        assertEquals(2, result.getContent().size());
        assertEquals("1", result.getContent().get(0).getPriority());
        assertEquals("3", result.getContent().get(1).getPriority());
        verify(emailRepository).filterEmailsBySubject(eq(userEmail), eq("Invoice"), any(Pageable.class));
    }

    @Test
    void shouldFilterOutboxByReceiverAndSortByDate() {
        String userEmail = "sender@seamail.com";
        LocalDateTime now = LocalDateTime.now();
        Email older = new Email(userEmail, "specific@b.com", "Old", "B", "1",
                now.minusDays(2), false);
        Email newer = new Email(userEmail, "specific@b.com", "New", "B", "1",
                now, false);

        when(emailRepository.filterOutboxByReceiver(eq(userEmail), eq("specific@b.com"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(new ArrayList<>(List.of(newer, older))));

        Page<EmailResponseDto> result = emailService.queryEmails(userEmail, "date", "sender", "specific@b.com", "Outbox", Pageable.unpaged());

        assertEquals(2, result.getContent().size());
        assertTrue(result.getContent().get(0).getDate().isBefore(result.getContent().get(1).getDate()) ||
                   result.getContent().get(0).getDate().isEqual(result.getContent().get(1).getDate()));
        verify(emailRepository).filterOutboxByReceiver(eq(userEmail), eq("specific@b.com"), any(Pageable.class));
    }

    @Test
    void shouldFilterTrashboxBySubjectAndSortByPriority() {
        String userEmail = "user@seamail.com";
        Email low = new Email("a@b.com", userEmail, "Invoice", "B", "3",
                LocalDateTime.now(), true);
        Email high = new Email("c@b.com", userEmail, "Invoice", "B", "1",
                LocalDateTime.now(), true);

        when(emailRepository.filterTrashBySubject(eq(userEmail), eq("Invoice"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(new ArrayList<>(List.of(low, high))));

        Page<EmailResponseDto> result = emailService.queryEmails(userEmail, "priority", "subject", "Invoice", "Trashbox", Pageable.unpaged());

        assertEquals(2, result.getContent().size());
        assertEquals("1", result.getContent().get(0).getPriority());
        assertEquals("3", result.getContent().get(1).getPriority());
        verify(emailRepository).filterTrashBySubject(eq(userEmail), eq("Invoice"), any(Pageable.class));
    }
}