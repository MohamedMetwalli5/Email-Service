package com.backendemailservice.backendemailservice.controller;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.backendemailservice.backendemailservice.dto.EmailRequestDto;
import com.backendemailservice.backendemailservice.dto.EmailResponseDto;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import com.backendemailservice.backendemailservice.dto.FilteringWrapper;
import com.backendemailservice.backendemailservice.dto.SortingWrapper;
import com.backendemailservice.backendemailservice.entity.Email;
import com.backendemailservice.backendemailservice.entity.User;
import com.backendemailservice.backendemailservice.service.EmailService;
import com.backendemailservice.backendemailservice.service.UserService;
import com.backendemailservice.backendemailservice.util.JwtUtil;

import jakarta.servlet.http.HttpServletRequest;


@RestController
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
    
    @PostMapping("/inbox")
    public ResponseEntity<List<EmailResponseDto>> loadInbox(HttpServletRequest request) {
        String authorizationHeader = request.getHeader("Authorization");

        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String token = authorizationHeader.substring(7);
        String email = jwtUtil.extractEmail(token);

        if (!jwtUtil.isTokenValid(token, email)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        List<EmailResponseDto> inboxEmails = emailService.loadInbox(new User(email, null))
        .stream()
        .map(e -> new EmailResponseDto(e.getEmailID(), e.getSender(), e.getReceiver(), e.getSubject(), e.getBody(), e.getPriority(), e.getDate(), e.getTrash()))
        .toList();

        return ResponseEntity.ok(inboxEmails);
    }
    
    @PostMapping("/outbox")
    public ResponseEntity<List<EmailResponseDto>> loadOutbox(HttpServletRequest request) {
        String authorizationHeader = request.getHeader("Authorization");

        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String token = authorizationHeader.substring(7);
        String email = jwtUtil.extractEmail(token);

        if (!jwtUtil.isTokenValid(token, email)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        List<EmailResponseDto> outboxEmails = emailService.loadOutbox(new User(email, null))
            .stream()
            .map(e -> new EmailResponseDto(e.getEmailID(), e.getSender(), e.getReceiver(), e.getSubject(), e.getBody(), e.getPriority(), e.getDate(), e.getTrash()))
            .toList();

        return ResponseEntity.ok(outboxEmails);
    }

    @PostMapping("/trashbox")
    public ResponseEntity<List<EmailResponseDto>> loadTrashbox(HttpServletRequest request) {
        String authorizationHeader = request.getHeader("Authorization");

        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String token = authorizationHeader.substring(7);
        String email = jwtUtil.extractEmail(token);

        if (!jwtUtil.isTokenValid(token, email)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        List<EmailResponseDto> trashboxEmails = emailService.loadTrashbox(new User(email, null))
            .stream()
            .map(e -> new EmailResponseDto(e.getEmailID(), e.getSender(), e.getReceiver(), e.getSubject(), e.getBody(), e.getPriority(), e.getDate(), e.getTrash()))
            .toList();

        return ResponseEntity.ok(trashboxEmails);
    }

    @PostMapping("/sendemail")
    public ResponseEntity<String> sendEmail(@Valid @RequestBody EmailRequestDto emailRequestDto, @RequestHeader("Authorization") String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String token = authorizationHeader.substring(7);
        String senderEmail = jwtUtil.extractEmail(token);

        if (!jwtUtil.isTokenValid(token, senderEmail)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        Optional<User> foundReceiver = userService.foundReceiver(emailRequestDto.getReceiver());
        if (foundReceiver.isPresent()) {
            Email email = new Email();
            email.setSender(senderEmail);
            email.setReceiver(emailRequestDto.getReceiver());
            email.setSubject(emailRequestDto.getSubject());
            email.setBody(emailRequestDto.getBody());
            email.setPriority(emailRequestDto.getPriority());
            email.setDate(java.time.LocalDate.now().toString());
            email.setTrash("No");
            emailService.createEmail(email);
            return ResponseEntity.ok("Email is sent!");
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Email wasn't sent! Receiver not found.");
        }
    }

    @PostMapping("/moveemailtotrashbox")
    public ResponseEntity<String> moveToTrashBox(@Valid @RequestBody EmailRequestDto emailRequestDto, @RequestHeader("Authorization") String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String token = authorizationHeader.substring(7);
        String userEmail = jwtUtil.extractEmail(token);

        if (!jwtUtil.isTokenValid(token, userEmail)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        emailService.moveToTrashBox(emailRequestDto.getEmailID());
        return ResponseEntity.ok("Moved email to trashbox!");
    }
    
    @PostMapping("/deleteemail")
    public ResponseEntity<String> deleteEmail(@Valid @RequestBody EmailRequestDto emailRequestDto, @RequestHeader("Authorization") String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String token = authorizationHeader.substring(7);
        String userEmail = jwtUtil.extractEmail(token);

        if (!jwtUtil.isTokenValid(token, userEmail)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        emailService.deleteEmail(emailRequestDto.getEmailID());
        return ResponseEntity.ok("Email is deleted!");
    }

    @PostMapping("/sortemails")
    public ResponseEntity<List<EmailResponseDto>> sortEmails(@RequestBody SortingWrapper sortingWrapper, @RequestHeader("Authorization") String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String token = authorizationHeader.substring(7);

        if (!jwtUtil.isTokenValid(token, sortingWrapper.getUser().getEmail())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        List<EmailResponseDto> sortedEmails = emailService.sortEmails(sortingWrapper)
            .stream()
            .map(e -> new EmailResponseDto(e.getEmailID(), e.getSender(), e.getReceiver(), e.getSubject(), e.getBody(), e.getPriority(), e.getDate(), e.getTrash()))
            .toList();

        return ResponseEntity.ok(sortedEmails);
    }

    @PostMapping("/filteremails")
    public ResponseEntity<List<EmailResponseDto>> filterEmails(@RequestBody FilteringWrapper filteringWrapper, @RequestHeader("Authorization") String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String token = authorizationHeader.substring(7);

        if (!jwtUtil.isTokenValid(token, filteringWrapper.getUser().getEmail())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        List<EmailResponseDto> filteredEmails = emailService.filterEmails(filteringWrapper)
            .stream()
            .map(e -> new EmailResponseDto(e.getEmailID(), e.getSender(), e.getReceiver(), e.getSubject(), e.getBody(), e.getPriority(), e.getDate(), e.getTrash()))
            .toList();

        return ResponseEntity.ok(filteredEmails);
    }
}
