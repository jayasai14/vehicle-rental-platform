package com.vehiclerental.common;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(CustomExceptions.ResourceNotFoundException.class)
    public ResponseEntity<ErrorDetails> handleResourceNotFound(CustomExceptions.ResourceNotFoundException ex, WebRequest request) {
        ErrorDetails details = new ErrorDetails(
                HttpStatus.NOT_FOUND.value(),
                "Not Found",
                ex.getMessage(),
                getPath(request)
        );
        return new ResponseEntity<>(details, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(CustomExceptions.BadRequestException.class)
    public ResponseEntity<ErrorDetails> handleBadRequest(CustomExceptions.BadRequestException ex, WebRequest request) {
        ErrorDetails details = new ErrorDetails(
                HttpStatus.BAD_REQUEST.value(),
                "Bad Request",
                ex.getMessage(),
                getPath(request)
        );
        return new ResponseEntity<>(details, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(CustomExceptions.ConflictException.class)
    public ResponseEntity<ErrorDetails> handleConflict(CustomExceptions.ConflictException ex, WebRequest request) {
        ErrorDetails details = new ErrorDetails(
                HttpStatus.CONFLICT.value(),
                "Conflict",
                ex.getMessage(),
                getPath(request)
        );
        return new ResponseEntity<>(details, HttpStatus.CONFLICT);
    }

    @ExceptionHandler(CustomExceptions.InvalidStateTransitionException.class)
    public ResponseEntity<ErrorDetails> handleInvalidState(CustomExceptions.InvalidStateTransitionException ex, WebRequest request) {
        ErrorDetails details = new ErrorDetails(
                HttpStatus.CONFLICT.value(),
                "Invalid State Transition",
                ex.getMessage(),
                getPath(request)
        );
        return new ResponseEntity<>(details, HttpStatus.CONFLICT);
    }

    @ExceptionHandler(CustomExceptions.ForbiddenException.class)
    public ResponseEntity<ErrorDetails> handleForbidden(CustomExceptions.ForbiddenException ex, WebRequest request) {
        ErrorDetails details = new ErrorDetails(
                HttpStatus.FORBIDDEN.value(),
                "Forbidden",
                ex.getMessage(),
                getPath(request)
        );
        return new ResponseEntity<>(details, HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorDetails> handleAccessDenied(AccessDeniedException ex, WebRequest request) {
        ErrorDetails details = new ErrorDetails(
                HttpStatus.FORBIDDEN.value(),
                "Access Denied",
                "You do not have permission to execute this operation",
                getPath(request)
        );
        return new ResponseEntity<>(details, HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorDetails> handleAuthentication(AuthenticationException ex, WebRequest request) {
        ErrorDetails details = new ErrorDetails(
                HttpStatus.UNAUTHORIZED.value(),
                "Unauthorized",
                ex.getMessage(),
                getPath(request)
        );
        return new ResponseEntity<>(details, HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorDetails> handleValidationErrors(MethodArgumentNotValidException ex, WebRequest request) {
        Map<String, String> fieldErrors = new HashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.put(error.getField(), error.getDefaultMessage());
        }
        ErrorDetails details = new ErrorDetails(
                HttpStatus.BAD_REQUEST.value(),
                "Validation Failed",
                "Request body contains validation errors",
                getPath(request),
                fieldErrors
        );
        return new ResponseEntity<>(details, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorDetails> handleGlobalException(Exception ex, WebRequest request) {
        ErrorDetails details = new ErrorDetails(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "Internal Server Error",
                ex.getMessage() != null ? ex.getMessage() : "An unexpected server error occurred",
                getPath(request)
        );
        return new ResponseEntity<>(details, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    private String getPath(WebRequest request) {
        if (request instanceof ServletWebRequest servletWebRequest) {
            return servletWebRequest.getRequest().getRequestURI();
        }
        return "";
    }

    public static class ErrorDetails {
        private int status;
        private String error;
        private String message;
        private String path;
        private Instant timestamp;
        private String correlationId;
        private Map<String, String> fieldErrors;

        public ErrorDetails(int status, String error, String message, String path) {
            this.status = status;
            this.error = error;
            this.message = message;
            this.path = path;
            this.timestamp = Instant.now();
            this.correlationId = UUID.randomUUID().toString();
        }

        public ErrorDetails(int status, String error, String message, String path, Map<String, String> fieldErrors) {
            this(status, error, message, path);
            this.fieldErrors = fieldErrors;
        }

        public int getStatus() { return status; }
        public String getError() { return error; }
        public String getMessage() { return message; }
        public String getPath() { return path; }
        public Instant getTimestamp() { return timestamp; }
        public String getCorrelationId() { return correlationId; }
        public Map<String, String> getFieldErrors() { return fieldErrors; }
    }
}
