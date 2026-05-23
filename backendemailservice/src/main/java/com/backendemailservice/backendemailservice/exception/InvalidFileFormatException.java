package com.backendemailservice.backendemailservice.exception;

// Domain exception with machine-readable errorCode
public class InvalidFileFormatException extends ApplicationException {
    public InvalidFileFormatException(String message) {
        super("INVALID_FILE_FORMAT", message);
    }
}
