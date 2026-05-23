package com.backendemailservice.backendemailservice.service;


import com.backendemailservice.backendemailservice.dto.EmailResponseDto;
import com.backendemailservice.backendemailservice.dto.SendEmailRequestDto;
import com.backendemailservice.backendemailservice.entity.Email;
import com.backendemailservice.backendemailservice.entity.User;

import java.util.List;

public interface IEmailService {
    // DTO-returning methods for controllers 
    List<EmailResponseDto> loadInboxDtos(String userEmail);
    List<EmailResponseDto> loadOutboxDtos(String userEmail);
    List<EmailResponseDto> loadTrashboxDtos(String userEmail);
    // sendEmail handles entity construction + receiver check
    void sendEmail(String senderEmail, SendEmailRequestDto request);
    // auth-check overloads
    void deleteEmail(Long emailID, String userEmail);
    void moveToTrashBox(Long emailID, String userEmail);
    // unified query with optional sort/filter params and mailbox context
    List<EmailResponseDto> queryEmails(String email, String sort, String filterBy, String filterValue, String mailbox);

    // Legacy methods (kept for existing callers)
    List<Email> loadInbox(User user);
    List<Email> loadOutbox(User user);
    List<Email> loadTrashbox(User user);
    void createEmail(Email email);
    void deleteEmail(Long emailID);
    void moveToTrashBox(Long emailID);
    List<Email> sortEmails(String receiverEmail, String sortingOption);
    List<Email> filterEmails(String receiverEmail, String filteringOption, String filteringValue);
    void evictInboxCache(String receiver);
}
