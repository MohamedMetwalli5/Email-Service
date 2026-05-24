package com.backendemailservice.backendemailservice.service;


import com.backendemailservice.backendemailservice.dto.EmailResponseDto;
import com.backendemailservice.backendemailservice.dto.SendEmailRequestDto;

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
}
