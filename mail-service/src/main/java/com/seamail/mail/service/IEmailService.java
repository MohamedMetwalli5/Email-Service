package com.seamail.mail.service;


import com.seamail.mail.dto.EmailResponseDto;
import com.seamail.mail.dto.SendEmailRequestDto;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface IEmailService {
    Page<EmailResponseDto> loadInboxDtos(String userEmail, Pageable pageable);
    Page<EmailResponseDto> loadOutboxDtos(String userEmail, Pageable pageable);
    Page<EmailResponseDto> loadTrashboxDtos(String userEmail, Pageable pageable);
    void sendEmail(String senderEmail, SendEmailRequestDto request);
    void deleteEmail(Long emailID, String userEmail);
    void moveToTrashBox(Long emailID, String userEmail);
    Page<EmailResponseDto> queryEmails(String email, String sort, String filterBy,
                                       String filterValue, String mailbox, Pageable pageable);
}
