package com.banking.transaction.controller;

import com.banking.transaction.dto.AccountCreateRequest;
import com.banking.transaction.dto.DepositRequest;
import com.banking.transaction.entity.Account;
import com.banking.transaction.exception.AccountNotFoundException;
import com.banking.transaction.exception.InsufficientFundsException;
import com.banking.transaction.service.AccountService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AccountController.class)
class AccountControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AccountService accountService;

    @Autowired
    private ObjectMapper objectMapper;

    private final String USER_ID_HEADER = "X-User-Id";
    private final Long TEST_USER_ID = 1L;

    @Test
    void createAccount_WhenRequestIsValid_ShouldReturnCreatedAccount() throws Exception {
        // Arrange
        AccountCreateRequest request = new AccountCreateRequest();
        request.setCurrency("TRY");

        Account account = new Account();
        account.setId(1L);
        account.setUserId(TEST_USER_ID);
        account.setCurrency("TRY");
        account.setBalance(BigDecimal.ZERO);
        account.setIban("TR123456");

        when(accountService.createAccount(any(AccountCreateRequest.class), eq(TEST_USER_ID))).thenReturn(account);

        // Act & Assert
        mockMvc.perform(post("/api/v1/accounts")
                        .header(USER_ID_HEADER, TEST_USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.currency").value("TRY"))
                .andExpect(jsonPath("$.message").value("Account created successfully."));
    }

    @Test
    void createAccount_WhenUserIdHeaderIsMissing_ShouldReturnBadRequest() throws Exception {
        // Arrange
        AccountCreateRequest request = new AccountCreateRequest();
        request.setCurrency("TRY");

        // Act & Assert
        mockMvc.perform(post("/api/v1/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest()); // @RequestHeader(required=true) ise 400 döner
    }

    @Test
    void createAccount_WhenAccountServiceThrowsException_ShouldReturnInternalServerError() throws Exception {
        // Arrange
        AccountCreateRequest request = new AccountCreateRequest();
        request.setCurrency("TRY");

        when(accountService.createAccount(any(AccountCreateRequest.class), eq(TEST_USER_ID)))
                .thenThrow(new RuntimeException("Account creation failed"));

        // Act & Assert
        mockMvc.perform(post("/api/v1/accounts")
                        .header(USER_ID_HEADER, TEST_USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void getAccounts_WhenAccountsExist_ShouldReturnAccountList() throws Exception {
        // Arrange
        Account account = new Account();
        account.setId(1L);
        account.setUserId(TEST_USER_ID);
        account.setBalance(BigDecimal.TEN);

        when(accountService.getAccountsByUserId(TEST_USER_ID)).thenReturn(List.of(account));

        // Act & Assert
        mockMvc.perform(get("/api/v1/accounts")
                        .header(USER_ID_HEADER, TEST_USER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(1))
                .andExpect(jsonPath("$.message").value("Accounts retrieved successfully."));
    }

    @Test
    void getAccounts_WhenNoAccountsExist_ShouldReturnEmptyList() throws Exception {
        // Arrange
        when(accountService.getAccountsByUserId(TEST_USER_ID)).thenReturn(Collections.emptyList());

        // Act & Assert
        mockMvc.perform(get("/api/v1/accounts")
                        .header(USER_ID_HEADER, TEST_USER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isEmpty())
                .andExpect(jsonPath("$.message").value("Accounts retrieved successfully."));
    }

    @Test
    void getAccounts_WhenUserIdHeaderIsMissing_ShouldReturnBadRequest() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/v1/accounts"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deposit_WhenRequestIsValid_ShouldReturnUpdatedAccount() throws Exception {
        // Arrange
        String iban = "TR123456";
        DepositRequest request = new DepositRequest();
        request.setAmount(new BigDecimal("100.00"));

        Account account = new Account();
        account.setIban(iban);
        account.setBalance(new BigDecimal("100.00"));

        when(accountService.depositToAccount(eq(iban), any(DepositRequest.class))).thenReturn(account);

        // Act & Assert
        mockMvc.perform(post("/api/v1/accounts/{iban}/deposits", iban)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.balance").value(100.00))
                .andExpect(jsonPath("$.message").value("Deposit successful."));
    }

    @Test
    void deposit_WhenAccountNotFound_ShouldReturnNotFound() throws Exception {
        // Arrange
        String iban = "TR_NOT_FOUND";
        DepositRequest request = new DepositRequest();
        request.setAmount(new BigDecimal("50.00"));

        when(accountService.depositToAccount(eq(iban), any(DepositRequest.class)))
                .thenThrow(new AccountNotFoundException("Account not found"));

        // Act & Assert
        mockMvc.perform(post("/api/v1/accounts/{iban}/deposits", iban)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound()); // Assuming GlobalExceptionHandler maps AccountNotFoundException to 404
    }

    @Test
    void deposit_WhenDepositRequestIsInvalid_ShouldReturnBadRequest() throws Exception {
        // Arrange
        String iban = "TR123456";
        DepositRequest request = new DepositRequest();
        request.setAmount(new BigDecimal("-10.00")); // Invalid amount

        // Act & Assert
        mockMvc.perform(post("/api/v1/accounts/{iban}/deposits", iban)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest()); // @Valid anotasyonu sayesinde 400 döner
    }
}