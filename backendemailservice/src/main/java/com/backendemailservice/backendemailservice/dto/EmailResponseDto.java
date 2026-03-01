package com.backendemailservice.backendemailservice.dto;

import java.time.LocalDateTime;

public class EmailResponseDto {

    private Integer emailID;
    private String sender;
    private String receiver;
    private String subject;
    private String body;
    private String priority;
    private LocalDateTime date;
    private boolean trash;

    public EmailResponseDto(Integer emailID, String sender, String receiver, String subject, String body, String priority, LocalDateTime date, boolean trash) {
        this.emailID = emailID;
        this.sender = sender;
        this.receiver = receiver;
        this.subject = subject;
        this.body = body;
        this.priority = priority;
        this.date = date;
        this.trash = trash;
    }

    public Integer getEmailID() { return emailID; }
    public String getSender() { return sender; }
    public String getReceiver() { return receiver; }
    public String getSubject() { return subject; }
    public String getBody() { return body; }
    public String getPriority() { return priority; }
    public LocalDateTime getDate() { return date; }
    public boolean isTrash() { return trash; }
}