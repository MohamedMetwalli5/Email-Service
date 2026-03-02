package com.backendemailservice.backendemailservice.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class SendEmailRequestDto {

    @NotBlank(message = "Receiver must not be blank")
    @Email(message = "Receiver must be a valid email address")
    private String receiver;

    @NotBlank(message = "Subject must not be blank")
    @Size(max = 255, message = "Subject must not exceed 255 characters")
    private String subject;

    @NotBlank(message = "Body must not be blank")
    @Size(max = 5000, message = "Body must not exceed 5000 characters")
    private String body;

    @NotBlank(message = "Priority must not be blank")
    private String priority;

    public SendEmailRequestDto() {}

    public String getReceiver() { return receiver; }
    public void setReceiver(String receiver) { this.receiver = receiver; }

    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }

    public String getBody() { return body; }
    public void setBody(String body) { this.body = body; }

    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }
}