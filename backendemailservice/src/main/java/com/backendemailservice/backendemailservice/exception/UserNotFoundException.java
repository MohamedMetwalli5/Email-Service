package com.backendemailservice.backendemailservice.exception;

// Domain exception with machine-readable errorCode
public class UserNotFoundException extends ApplicationException {
    public UserNotFoundException(String message) {
        super("USER_NOT_FOUND", message);
    }
}
