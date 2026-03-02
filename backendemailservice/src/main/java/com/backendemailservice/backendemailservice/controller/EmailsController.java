package com.backendemailservice.backendemailservice.controller;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.backendemailservice.backendemailservice.dto.*;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.backendemailservice.backendemailservice.exception.ReceiverNotFoundException;
import com.backendemailservice.backendemailservice.entity.Email;
import com.backendemailservice.backendemailservice.entity.User;
import com.backendemailservice.backendemailservice.service.EmailService;
import com.backendemailservice.backendemailservice.service.UserService;
import com.backendemailservice.backendemailservice.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/v1")
public class EmailsController {

    private final EmailService emailService;
    private final UserService userService;
    private final JwtUtil jwtUtil;

    @Autowired
    public EmailsController(EmailService emailService, UserService userService, JwtUtil jwtUtil) {
        this.emailService = emailService;
        this.userService = userService;
        this.jwtUtil = jwtUtil;
    }

    @GetMapping("/inbox")
    public ResponseEntity<List<EmailResponseDto>> loadInbox(HttpServletRequest request) {
        String email = jwtUtil.extractAndValidateToken(request.getHeader("Authorization"));
        if (email == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        List<EmailResponseDto> inboxEmails = emailService.loadInbox(new User(email, null))
                .stream()
                .map(e -> new EmailResponseDto(e.getEmailID(), e.getSender(), e.getReceiver(), e.getSubject(), e.getBody(), e.getPriority(), e.getDate(), e.isTrash()))
                .toList();
        return ResponseEntity.ok(inboxEmails);
    }

    @GetMapping("/outbox")
    public ResponseEntity<List<EmailResponseDto>> loadOutbox(HttpServletRequest request) {
        String email = jwtUtil.extractAndValidateToken(request.getHeader("Authorization"));
        if (email == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        List<EmailResponseDto> outboxEmails = emailService.loadOutbox(new User(email, null))
                .stream()
                .map(e -> new EmailResponseDto(e.getEmailID(), e.getSender(), e.getReceiver(), e.getSubject(), e.getBody(), e.getPriority(), e.getDate(), e.isTrash()))
                .toList();
        return ResponseEntity.ok(outboxEmails);
    }

    @GetMapping("/trashbox")
    public ResponseEntity<List<EmailResponseDto>> loadTrashbox(HttpServletRequest request) {
        String email = jwtUtil.extractAndValidateToken(request.getHeader("Authorization"));
        if (email == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        List<EmailResponseDto> trashboxEmails = emailService.loadTrashbox(new User(email, null))
                .stream()
                .map(e -> new EmailResponseDto(e.getEmailID(), e.getSender(), e.getReceiver(), e.getSubject(), e.getBody(), e.getPriority(), e.getDate(), e.isTrash()))
                .toList();
        return ResponseEntity.ok(trashboxEmails);
    }

    @PostMapping("/send-email")
    public ResponseEntity<?> sendEmail(
            @RequestHeader("Authorization") String authorizationHeader,
            @Valid @RequestBody SendEmailRequestDto request) {

        String senderEmail = jwtUtil.extractAndValidateToken(authorizationHeader);
        if (senderEmail == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Unauthorized"));
        }

        if (userService.foundReceiver(request.getReceiver()).isEmpty()) {
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

        emailService.createEmail(email);
        return ResponseEntity.ok(Map.of("message", "Email is sent!"));
    }

    @PostMapping("/move-to-trash")
    public ResponseEntity<?> moveEmailToTrashbox(
            @RequestHeader("Authorization") String authorizationHeader,
            @Valid @RequestBody EmailActionRequestDto request) {

        String tokenEmail = jwtUtil.extractAndValidateToken(authorizationHeader);
        if (tokenEmail == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Unauthorized"));
        }

        emailService.moveToTrashBox(request.getEmailId());
        return ResponseEntity.ok(Map.of("message", "Moved email to trashbox!"));
    }

    @DeleteMapping("/delete-email")
    public ResponseEntity<?> deleteEmail(
            @RequestHeader("Authorization") String authorizationHeader,
            @Valid @RequestBody EmailActionRequestDto request) {

        String tokenEmail = jwtUtil.extractAndValidateToken(authorizationHeader);
        if (tokenEmail == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Unauthorized"));
        }

        emailService.deleteEmail(request.getEmailId());
        return ResponseEntity.ok(Map.of("message", "Email is deleted!"));
    }

    @PostMapping("/sort-emails")
    public ResponseEntity<?> sortEmails(
            @RequestHeader("Authorization") String authorizationHeader,
            @Valid @RequestBody SortEmailsRequestDto request) {

        String tokenEmail = jwtUtil.extractAndValidateToken(authorizationHeader);
        if (tokenEmail == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Unauthorized"));
        }

        List<EmailResponseDto> sorted = emailService.sortEmails(tokenEmail, request.getSortingOption())
                .stream()
                .map(email -> new EmailResponseDto(email.getEmailID(), email.getSender(),
                        email.getReceiver(), email.getSubject(), email.getBody(),
                        email.getPriority(), email.getDate(), email.isTrash()))
                .toList();

        return ResponseEntity.ok(sorted);
    }

    @PostMapping("/filter-emails")
    public ResponseEntity<?> filterEmails(
            @RequestHeader("Authorization") String authorizationHeader,
            @Valid @RequestBody FilterEmailsRequestDto request) {

        String tokenEmail = jwtUtil.extractAndValidateToken(authorizationHeader);
        if (tokenEmail == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Unauthorized"));
        }

        List<EmailResponseDto> filtered = emailService.filterEmails(tokenEmail, request.getFilteringOption(), request.getFilteringValue())
                .stream()
                .map(email -> new EmailResponseDto(email.getEmailID(), email.getSender(),
                        email.getReceiver(), email.getSubject(), email.getBody(),
                        email.getPriority(), email.getDate(), email.isTrash()))
                .toList();

        return ResponseEntity.ok(filtered);
    }
}