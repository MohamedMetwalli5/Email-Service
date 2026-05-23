package com.backendemailservice.backendemailservice.exception;

// Domain exception with machine-readable errorCode
public class ReceiverNotFoundException extends ApplicationException {
    public ReceiverNotFoundException(String message) {
        super("RECEIVER_NOT_FOUND", message);
    }
}
