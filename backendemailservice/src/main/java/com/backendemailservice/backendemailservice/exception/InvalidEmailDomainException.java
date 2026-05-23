package com.backendemailservice.backendemailservice.exception;

// Domain exception with machine-readable errorCode
public class InvalidEmailDomainException extends ApplicationException {
    public InvalidEmailDomainException(String message) {
        super("INVALID_EMAIL_DOMAIN", message);
    }
}
