package com.backendemailservice.backendemailservice.dto;

import jakarta.validation.constraints.NotBlank;

public class EmailRequestDto {

    private Integer emailID;

    @NotBlank(message = "Receiver is required")
    private String receiver;

    @NotBlank(message = "Subject is required")
    private String subject;

    @NotBlank(message = "Body is required")
    private String body;

    private String priority;

    public Integer getEmailID() { return emailID; }
    public void setEmailID(Integer emailID) { this.emailID = emailID; }

    public String getReceiver() { return receiver; }
    public void setReceiver(String receiver) { this.receiver = receiver; }

    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }

    public String getBody() { return body; }
    public void setBody(String body) { this.body = body; }

    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }
}