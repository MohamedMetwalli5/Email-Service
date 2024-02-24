package com.backendemailservice.backendemailservice.controller;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.backendemailservice.backendemailservice.entity.Email;
import com.backendemailservice.backendemailservice.entity.User;
import com.backendemailservice.backendemailservice.service.EmailService;


@RestController
public class EmailsController {
    private final EmailService emailService;

    @Autowired
    public EmailsController(EmailService emailService) {
        this.emailService = emailService;
    }
    
    @PostMapping("/inbox")
    public List<Email> loadInbox(@RequestBody User user) {
    	List<Email> inboxEmails = emailService.loadInbox(user);
        return inboxEmails;
    }
    
    @PostMapping("/outbox")
    public List<Email> loadOutbox(@RequestBody User user) {
    	List<Email> outboxEmails = emailService.loadOutbox(user);
        return outboxEmails;
    }
    
}
