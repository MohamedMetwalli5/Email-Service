package com.backendemailservice.backendemailservice.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public class DeleteAccountRequestDto {

    @NotBlank(message = "Email must not be blank")
    @Email(message = "Must be a valid email address")
    private String email;

    public DeleteAccountRequestDto() {}

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
}