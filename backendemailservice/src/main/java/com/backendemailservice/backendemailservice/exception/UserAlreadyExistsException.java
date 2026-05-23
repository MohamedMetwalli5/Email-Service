package com.backendemailservice.backendemailservice.exception;

// Domain exception with machine-readable errorCode
public class UserAlreadyExistsException extends ApplicationException {
    public UserAlreadyExistsException(String message) {
        super("USER_ALREADY_EXISTS", message);
    }
}
