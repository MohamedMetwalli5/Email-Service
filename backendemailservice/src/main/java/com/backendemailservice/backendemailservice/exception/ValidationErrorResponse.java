package com.backendemailservice.backendemailservice.exception;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;
import java.util.List;

// Consistent validation error response shape
public record ValidationErrorResponse(
        int status,
        String error,
        String message,
        String path,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime timestamp,
        List<String> fieldErrors
) {}
