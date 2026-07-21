package com.seamail.mail.controller;

import com.seamail.mail.dto.EmailActionRequestDto;
import com.seamail.mail.dto.EmailResponseDto;
import com.seamail.mail.dto.SendEmailRequestDto;
import com.seamail.mail.service.IEmailService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;



@RestController
@RequestMapping("/api/v1")
@Validated
public class EmailsController {

    private final IEmailService emailService;

    public EmailsController(IEmailService emailService) {
        this.emailService = emailService;
    }

    @GetMapping("/inbox")
    public ResponseEntity<Page<EmailResponseDto>> loadInbox(
            @AuthenticationPrincipal(expression = "subject") String email,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(emailService.loadInboxDtos(email, pageable));
    }

    @GetMapping("/outbox")
    public ResponseEntity<Page<EmailResponseDto>> loadOutbox(
            @AuthenticationPrincipal(expression = "subject") String email,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(emailService.loadOutboxDtos(email, pageable));
    }

    @GetMapping("/trashbox")
    public ResponseEntity<Page<EmailResponseDto>> loadTrashbox(
            @AuthenticationPrincipal(expression = "subject") String email,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(emailService.loadTrashboxDtos(email, pageable));
    }

    @PostMapping("/send-email")
    public ResponseEntity<Void> sendEmail(
            @AuthenticationPrincipal(expression = "subject") String senderEmail,
            @Valid @RequestBody SendEmailRequestDto request) {
        emailService.sendEmail(senderEmail, request);
        return ResponseEntity.status(201).build();
    }

    @PostMapping("/move-to-trash")
    public ResponseEntity<Void> moveEmailToTrashbox(
            @AuthenticationPrincipal(expression = "subject") String email,
            @Valid @RequestBody EmailActionRequestDto request) {
        emailService.moveToTrashBox(request.getEmailId(), email);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/delete-email")
    public ResponseEntity<Void> deleteEmail(
            @AuthenticationPrincipal(expression = "subject") String email,
            @Valid @RequestBody EmailActionRequestDto request) {
        emailService.deleteEmail(request.getEmailId(), email);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/emails")
    public ResponseEntity<Page<EmailResponseDto>> queryEmails(
            @AuthenticationPrincipal(expression = "subject") String email,
            @RequestParam(required = false) String sort,
            @RequestParam(required = false) String filterBy,
            @RequestParam(required = false) String filterValue,
            @RequestParam(required = false) String mailbox,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(emailService.queryEmails(email, sort, filterBy, filterValue, mailbox, pageable));
    }
}
