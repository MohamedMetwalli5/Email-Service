package com.backendemailservice.backendemailservice.controller;

import com.backendemailservice.backendemailservice.dto.EmailActionRequestDto;
import com.backendemailservice.backendemailservice.dto.EmailResponseDto;
import com.backendemailservice.backendemailservice.dto.SendEmailRequestDto;
import com.backendemailservice.backendemailservice.service.IEmailService;
import jakarta.validation.Valid;
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

import java.util.List;



@RestController
@RequestMapping("/api/v1")
@Validated
public class EmailsController {

    private final IEmailService emailService;

    public EmailsController(IEmailService emailService) {
        this.emailService = emailService;
    }

    @GetMapping("/inbox")
    public ResponseEntity<List<EmailResponseDto>> loadInbox(
            @AuthenticationPrincipal String email) {
        return ResponseEntity.ok(emailService.loadInboxDtos(email));
    }

    @GetMapping("/outbox")
    public ResponseEntity<List<EmailResponseDto>> loadOutbox(
            @AuthenticationPrincipal String email) {
        return ResponseEntity.ok(emailService.loadOutboxDtos(email));
    }

    @GetMapping("/trashbox")
    public ResponseEntity<List<EmailResponseDto>> loadTrashbox(
            @AuthenticationPrincipal String email) {
        return ResponseEntity.ok(emailService.loadTrashboxDtos(email));
    }

    @PostMapping("/send-email")
    public ResponseEntity<Void> sendEmail(
            @AuthenticationPrincipal String senderEmail,
            @Valid @RequestBody SendEmailRequestDto request) {
        emailService.sendEmail(senderEmail, request);
        return ResponseEntity.status(201).build();
    }

    @PostMapping("/move-to-trash")
    public ResponseEntity<Void> moveEmailToTrashbox(
            @AuthenticationPrincipal String email,
            @Valid @RequestBody EmailActionRequestDto request) {
        emailService.moveToTrashBox(request.getEmailId(), email);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/delete-email")
    public ResponseEntity<Void> deleteEmail(
            @AuthenticationPrincipal String email,
            @Valid @RequestBody EmailActionRequestDto request) {
        emailService.deleteEmail(request.getEmailId(), email);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/emails")
    public ResponseEntity<List<EmailResponseDto>> queryEmails(
            @AuthenticationPrincipal String email,
            @RequestParam(required = false) String sort,
            @RequestParam(required = false) String filterBy,
            @RequestParam(required = false) String filterValue,
            @RequestParam(required = false) String mailbox) {
        return ResponseEntity.ok(emailService.queryEmails(email, sort, filterBy, filterValue, mailbox));
    }
}
