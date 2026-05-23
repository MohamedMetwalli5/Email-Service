package com.backendemailservice.backendemailservice.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;



@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // --- Validation ---

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ValidationErrorResponse> handleValidationException(
            MethodArgumentNotValidException ex, HttpServletRequest request) {

        List<String> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                .map(err -> err.getField() + ": " + err.getDefaultMessage())
                .collect(Collectors.toList());

        ValidationErrorResponse body = new ValidationErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                "VALIDATION_FAILED",
                "Request validation failed.",
                request.getRequestURI(),
                LocalDateTime.now(),
                fieldErrors
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    // --- Domain exceptions (specific -> general) ---

    @ExceptionHandler({UserNotFoundException.class, EmailNotFoundException.class,
                        ReceiverNotFoundException.class})
    public ResponseEntity<ErrorResponse> handleNotFound(ApplicationException ex,
                                                         HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ErrorResponse.of(HttpStatus.NOT_FOUND, ex.getErrorCode(),
                        ex.getMessage(), request.getRequestURI()));
    }

    @ExceptionHandler(UserAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handleConflict(ApplicationException ex,
                                                         HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ErrorResponse.of(HttpStatus.CONFLICT, ex.getErrorCode(),
                        ex.getMessage(), request.getRequestURI()));
    }

    @ExceptionHandler({InvalidEmailDomainException.class, InvalidFileFormatException.class})
    public ResponseEntity<ErrorResponse> handleBadRequest(ApplicationException ex,
                                                           HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.of(HttpStatus.BAD_REQUEST, ex.getErrorCode(),
                        ex.getMessage(), request.getRequestURI()));
    }

    // --- ResponseStatusException (preserves status code) ---

    // Consistent error response shape
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ErrorResponse> handleResponseStatus(ResponseStatusException ex,
                                                               HttpServletRequest request) {
        log.warn("Response status exception on [{}]: {}", request.getRequestURI(), ex.getMessage());
        return ResponseEntity.status(ex.getStatusCode())
                .body(ErrorResponse.of(HttpStatus.valueOf(ex.getStatusCode().value()),
                        "REQUEST_ERROR", ex.getReason(), request.getRequestURI()));
    }

    // --- Catch-all ---

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex,
                                                                 HttpServletRequest request) {
        log.error("Unhandled exception on [{}]: {}", request.getRequestURI(), ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorResponse.of(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR",
                        "An unexpected error occurred. Please try again later.",
                        request.getRequestURI()));
    }
}
