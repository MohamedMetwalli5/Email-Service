package com.backendemailservice.backendemailservice.dto;

public class UserResponseDto {

    private String email;
    private String language;

    public UserResponseDto(String email, String language) {
        this.email = email;
        this.language = language;
    }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }
}