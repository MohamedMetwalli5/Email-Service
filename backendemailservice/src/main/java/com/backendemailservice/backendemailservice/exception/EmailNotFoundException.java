package com.backendemailservice.backendemailservice.exception;

// Domain exception with machine-readable errorCode
public class EmailNotFoundException extends ApplicationException {
    public EmailNotFoundException(String message) {
        super("EMAIL_NOT_FOUND", message);
    }
}
