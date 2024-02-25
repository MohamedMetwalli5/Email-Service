package com.backendemailservice.backendemailservice.controller;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.backendemailservice.backendemailservice.SortingWrapper;
import com.backendemailservice.backendemailservice.entity.Email;
import com.backendemailservice.backendemailservice.entity.User;
import com.backendemailservice.backendemailservice.service.EmailService;
import com.backendemailservice.backendemailservice.service.UserService;


@RestController
public class EmailsController {
    private final EmailService emailService;
    private final UserService userService;

    @Autowired
    public EmailsController(EmailService emailService, UserService userService) {
        this.emailService = emailService;
        this.userService = userService;
    }
    
    @CrossOrigin(origins = "http://localhost:8080")
    @PostMapping("/inbox")
    public List<Email> loadInbox(@RequestBody User user) {
    	List<Email> inboxEmails = emailService.loadInbox(user);
        return inboxEmails;
    }
    
    @CrossOrigin(origins = "http://localhost:8080")
    @PostMapping("/outbox")
    public List<Email> loadOutbox(@RequestBody User user) {
    	List<Email> outboxEmails = emailService.loadOutbox(user);
        return outboxEmails;
    }
    
    @CrossOrigin(origins = "http://localhost:8080")
    @PostMapping("/trashbox")
    public List<Email> loadTrashbox(@RequestBody User user) {
    	List<Email> trashboxEmails = emailService.loadTrashbox(user);
        return trashboxEmails;
    }
    
    @CrossOrigin(origins = "http://localhost:8080")
    @PostMapping("/sendemail")
    public ResponseEntity<String> sendEmail(@RequestBody Email email) {
    	Optional<User> foundReceiver = userService.foundReceiver(email.getReceiver());
        if (foundReceiver.isPresent()) {
            // if receiver exist in the database
        	emailService.createEmail(email);
            return ResponseEntity.ok().body("Email is sent!");
        } else {
            // if receiver doesn't exist
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Email wasn't sent!");
        }
    }
    
    @CrossOrigin(origins = "http://localhost:8080")
    @PostMapping("/deleteemail")
    public ResponseEntity<String> deleteEmail(@RequestBody Email email) {
    	emailService.deleteEmail(email);
        return ResponseEntity.ok().body("Email is deleted!");    
    }
    
    @CrossOrigin(origins = "http://localhost:8080")
    @PostMapping("/sortemails")
    public List<Email> sortEmails(@RequestBody SortingWrapper sortingWrapper) {
    	List<Email> sortedEmails = emailService.sortEmails(sortingWrapper);
        return sortedEmails;  
    }
    
}
