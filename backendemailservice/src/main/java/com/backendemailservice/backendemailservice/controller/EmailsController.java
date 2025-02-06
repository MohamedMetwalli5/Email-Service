package com.backendemailservice.backendemailservice.controller;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import com.backendemailservice.backendemailservice.FilteringWrapper;
import com.backendemailservice.backendemailservice.SortingWrapper;
import com.backendemailservice.backendemailservice.entity.Email;
import com.backendemailservice.backendemailservice.entity.User;
import com.backendemailservice.backendemailservice.service.EmailService;
import com.backendemailservice.backendemailservice.service.UserService;
import com.backendemailservice.backendemailservice.util.JwtUtil;

import jakarta.servlet.http.HttpServletRequest;


//@CrossOrigin(origins = "http://localhost:8080")
@RestController
public class EmailsController {
	@Value("${cors.allowed.origin}")
    private String allowedOrigin;
	
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
    public ResponseEntity<List<Email>> loadInbox(HttpServletRequest request) {
        String authorizationHeader = request.getHeader("Authorization");

        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String token = authorizationHeader.substring(7);
        String email = jwtUtil.extractEmail(token);

        if (!jwtUtil.isTokenValid(token, email)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        List<Email> inboxEmails = emailService.loadInbox(new User(email, null));
        return ResponseEntity.ok(inboxEmails);
    }
    
    @PostMapping("/outbox")
    public ResponseEntity<List<Email>> loadOutbox(HttpServletRequest request) {
    	String authorizationHeader = request.getHeader("Authorization");

        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String token = authorizationHeader.substring(7);
        String email = jwtUtil.extractEmail(token);

        if (!jwtUtil.isTokenValid(token, email)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        
        List<Email> outboxEmails = emailService.loadOutbox(new User(email, null));
        return ResponseEntity.ok(outboxEmails);
    }

    @PostMapping("/trashbox")
    public ResponseEntity<List<Email>> loadTrashbox(HttpServletRequest request) {
    	String authorizationHeader = request.getHeader("Authorization");

        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String token = authorizationHeader.substring(7);
        String email = jwtUtil.extractEmail(token);

        if (!jwtUtil.isTokenValid(token, email)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        List<Email> trashboxEmails = emailService.loadTrashbox(new User(email, null));
        return ResponseEntity.ok(trashboxEmails);
    }

    @PostMapping("/sendemail")
    public ResponseEntity<String> sendEmail(@RequestBody Email email, @RequestHeader("Authorization") String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String token = authorizationHeader.substring(7);

        if (!jwtUtil.isTokenValid(token, email.getSender())) {
        	System.out.println("Token validation failed for user: " + email.getSender());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        Optional<User> foundReceiver = userService.foundReceiver(email.getReceiver());
        if (foundReceiver.isPresent()) {
            emailService.createEmail(email);
            return ResponseEntity.ok("Email is sent!");
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Email wasn't sent! Receiver not found.");
        }
    }

    @PostMapping("/moveemailtotrashbox")
    public ResponseEntity<String> moveToTrashBox(@RequestBody Email email, @RequestHeader("Authorization") String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String token = authorizationHeader.substring(7);

        if (!jwtUtil.isTokenValid(token, email.getReceiver())) {
        	System.out.println("Token validation failed for user: " + email.getReceiver());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        emailService.moveToTrashBox(email);
        return ResponseEntity.ok("Moved email to trashbox!");
    }
    
    @PostMapping("/deleteemail")
    public ResponseEntity<String> deleteEmail(@RequestBody Email email, @RequestHeader("Authorization") String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String token = authorizationHeader.substring(7);

        if (!jwtUtil.isTokenValid(token, email.getReceiver())) {
        	System.out.println("Token validation failed for user: " + email.getReceiver());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        emailService.deleteEmail(email);
        return ResponseEntity.ok("Email is deleted!");
    }

    @PostMapping("/sortemails")
    public ResponseEntity<List<Email>> sortEmails(@RequestBody SortingWrapper sortingWrapper, @RequestHeader("Authorization") String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String token = authorizationHeader.substring(7);

        if (!jwtUtil.isTokenValid(token, sortingWrapper.getUser().getEmail())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        List<Email> sortedEmails = emailService.sortEmails(sortingWrapper);
        
        return ResponseEntity.ok(sortedEmails);
    }

    @PostMapping("/filteremails")
    public ResponseEntity<List<Email>> filterEmails(@RequestBody FilteringWrapper filteringWrapper, @RequestHeader("Authorization") String authorizationHeader) {
    	if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String token = authorizationHeader.substring(7);

        if (!jwtUtil.isTokenValid(token, filteringWrapper.getUser().getEmail())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        List<Email> filteredEmails = emailService.filterEmails(filteringWrapper);
        
        return ResponseEntity.ok(filteredEmails);
    }
    
}
