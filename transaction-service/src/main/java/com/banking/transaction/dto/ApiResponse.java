package com.banking.transaction.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ApiResponse<T> {

    private String message;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS")
    private LocalDateTime timestamp;
    private T data;

    public ApiResponse(String message, T data) {
        this.message = message;
        this.data = data;
        this.timestamp = LocalDateTime.now();
    }
}
