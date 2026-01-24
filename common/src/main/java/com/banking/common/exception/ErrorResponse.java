package com.banking.common.exception;

import java.time.LocalDateTime;

public class ErrorResponse {

    private String errorCode;
    private String message;
    private LocalDateTime timestamp;
    private String details;

    public ErrorResponse(String errorCode, String message, LocalDateTime timestamp, String details) {
        this.errorCode = errorCode;
        this.message = message;
        this.timestamp = timestamp;
        this.details = details;
    }

    // Getters and Setters
    public String getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }
}
