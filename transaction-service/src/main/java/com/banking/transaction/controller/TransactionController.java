package com.banking.transaction.controller;

import com.banking.transaction.dto.ApiResponse;
import com.banking.transaction.dto.TransactionRequest;
import com.banking.transaction.service.TransactionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/transactions")
public class TransactionController {

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Object>> createTransaction(@RequestHeader("X-Correlation-ID") String correlationId,
                                                                 @RequestHeader("X-User-Id") Long userId,
                                                                 @Valid @RequestBody TransactionRequest request) {
        transactionService.executeTransaction(request, correlationId, userId);
        ApiResponse<Object> response = new ApiResponse<>("Transaction accepted for processing.", Map.of("correlationId", correlationId));
        return new ResponseEntity<>(response, HttpStatus.ACCEPTED);
    }
}