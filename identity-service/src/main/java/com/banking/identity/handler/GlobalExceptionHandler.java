package com.banking.identity.handler;

import jakarta.validation.ConstraintViolation;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;

import jakarta.validation.ConstraintViolationException;
import java.util.Map;
import java.util.stream.Collectors;

@ControllerAdvice
public class GlobalExceptionHandler {

    private static final String TIMESTAMP_KEY = "timestamp";
    private static final String STATUS_KEY = "status";
    private static final String ERROR_KEY = "error";
    private static final String MESSAGE_KEY = "message";
    private static final String PATH_KEY = "path";

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Object> handleConstraintViolation(
            ConstraintViolationException ex, WebRequest request) {

        Map<String, String> errors = ex.getConstraintViolations().stream()
                .collect(Collectors.toMap(
                        violation -> violation.getPropertyPath().toString(),
                        ConstraintViolation::getMessage
                ));

        Map<String, Object> body = Map.of(
                TIMESTAMP_KEY, System.currentTimeMillis(),
                STATUS_KEY, HttpStatus.BAD_REQUEST.value(),
                ERROR_KEY, "Bad Request",
                MESSAGE_KEY, "Validation failed",
                "errors", errors,
                PATH_KEY, request.getDescription(false).substring(4)
        );

        return new ResponseEntity<>(body, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(DuplicateKeyException.class)
    public ResponseEntity<Object> handleDuplicateKeyException(
            DuplicateKeyException ex, WebRequest request) {

        Map<String, Object> body = Map.of(
                TIMESTAMP_KEY, System.currentTimeMillis(),
                STATUS_KEY, HttpStatus.CONFLICT.value(),
                ERROR_KEY, "Conflict",
                MESSAGE_KEY, ex.getMessage(),
                PATH_KEY, request.getDescription(false).substring(4)
        );

        return new ResponseEntity<>(body, HttpStatus.CONFLICT);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<Object> handleBadCredentialsException(
            BadCredentialsException ex, WebRequest request) {

        Map<String, Object> body = Map.of(
                TIMESTAMP_KEY, System.currentTimeMillis(),
                STATUS_KEY, HttpStatus.UNAUTHORIZED.value(),
                ERROR_KEY, "Unauthorized",
                MESSAGE_KEY, ex.getMessage(),
                PATH_KEY, request.getDescription(false).substring(4)
        );

        return new ResponseEntity<>(body, HttpStatus.UNAUTHORIZED);
    }
}