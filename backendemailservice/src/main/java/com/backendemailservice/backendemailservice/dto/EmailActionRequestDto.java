package com.backendemailservice.backendemailservice.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public class EmailActionRequestDto {

    @NotNull(message = "Email ID must not be null")
    @Positive(message = "Email ID must be a positive number")
    private Integer emailId;

    public EmailActionRequestDto() {}

    public Integer getEmailId() { return emailId; }
    public void setEmailId(Integer emailId) { this.emailId = emailId; }
}