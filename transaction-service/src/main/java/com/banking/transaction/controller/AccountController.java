package com.banking.transaction.controller;

import com.banking.transaction.dto.AccountCreateRequest;
import com.banking.transaction.dto.ApiResponse;
import com.banking.transaction.dto.DepositRequest;
import com.banking.transaction.entity.Account;
import com.banking.transaction.service.AccountService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/accounts")
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Account>> createAccount(@RequestHeader("X-User-Id") Long userId, @RequestBody AccountCreateRequest request) {
        Account account = accountService.createAccount(request, userId);
        ApiResponse<Account> response = new ApiResponse<>("Account created successfully.", account);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<Account>>> getAccounts(@RequestHeader("X-User-Id") Long userId) {
        List<Account> accounts = accountService.getAccountsByUserId(userId);
        ApiResponse<List<Account>> response = new ApiResponse<>("Accounts retrieved successfully.", accounts);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{iban}/deposits")
    public ResponseEntity<ApiResponse<Account>> deposit(@PathVariable String iban, @Valid @RequestBody DepositRequest request) {
        Account account = accountService.depositToAccount(iban, request);
        ApiResponse<Account> response = new ApiResponse<>("Deposit successful.", account);
        return ResponseEntity.ok(response);
    }
}