package com.backendemailservice.backendemailservice.exception;

import com.fasterxml.jackson.annotation.JsonFormat;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;

// Consistent error response shape
public record ErrorResponse(
        int status,
        String error,
        String message,
        String path,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime timestamp
) {
    public static ErrorResponse of(HttpStatus status, String error,
                                   String message, String path) {
        return new ErrorResponse(
                status.value(), error, message, path, LocalDateTime.now()
        );
    }
}
