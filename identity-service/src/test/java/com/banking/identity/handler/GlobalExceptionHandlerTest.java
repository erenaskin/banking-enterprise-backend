package com.banking.identity.handler;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.context.request.WebRequest;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler globalExceptionHandler;
    private WebRequest webRequest;

    @BeforeEach
    void setUp() {
        globalExceptionHandler = new GlobalExceptionHandler();
        webRequest = mock(WebRequest.class);
        when(webRequest.getDescription(false)).thenReturn("uri=/test/path");
    }

    @Test
    void handleConstraintViolation_ShouldReturnBadRequest() {
        // Arrange
        ConstraintViolation<?> violation = mock(ConstraintViolation.class);
        Path path = mock(Path.class);
        when(path.toString()).thenReturn("fieldName");
        when(violation.getPropertyPath()).thenReturn(path);
        when(violation.getMessage()).thenReturn("must not be null");

        ConstraintViolationException ex = new ConstraintViolationException("Validation failed", Set.of(violation));

        // Act
        ResponseEntity<Object> response = globalExceptionHandler.handleConstraintViolation(ex, webRequest);

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        
        assertEquals(HttpStatus.BAD_REQUEST.value(), body.get("status"));
        assertEquals("Bad Request", body.get("error"));
        assertEquals("Validation failed", body.get("message"));
        assertEquals("/test/path", body.get("path"));
        
        @SuppressWarnings("unchecked")
        Map<String, String> errors = (Map<String, String>) body.get("errors");
        assertEquals("must not be null", errors.get("fieldName"));
    }

    @Test
    void handleDuplicateKeyException_ShouldReturnConflict() {
        // Arrange
        DuplicateKeyException ex = new DuplicateKeyException("Duplicate key error");

        // Act
        ResponseEntity<Object> response = globalExceptionHandler.handleDuplicateKeyException(ex, webRequest);

        // Assert
        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertNotNull(response.getBody());

        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();

        assertEquals(HttpStatus.CONFLICT.value(), body.get("status"));
        assertEquals("Conflict", body.get("error"));
        assertEquals("Duplicate key error", body.get("message"));
        assertEquals("/test/path", body.get("path"));
    }

    @Test
    void handleBadCredentialsException_ShouldReturnUnauthorized() {
        // Arrange
        BadCredentialsException ex = new BadCredentialsException("Invalid credentials");

        // Act
        ResponseEntity<Object> response = globalExceptionHandler.handleBadCredentialsException(ex, webRequest);

        // Assert
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertNotNull(response.getBody());

        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();

        assertEquals(HttpStatus.UNAUTHORIZED.value(), body.get("status"));
        assertEquals("Unauthorized", body.get("error"));
        assertEquals("Invalid credentials", body.get("message"));
        assertEquals("/test/path", body.get("path"));
    }
}