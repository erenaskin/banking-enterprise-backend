package com.banking.transaction.controller;

import com.banking.transaction.dto.AccountCreateRequest;
import com.banking.transaction.dto.DepositRequest;
import com.banking.transaction.entity.Account;
import com.banking.transaction.exception.AccountNotFoundException;
import com.banking.transaction.service.AccountService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
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
        AccountCreateRequest request = new AccountCreateRequest();
        request.setCurrency("TRY");

        Account account = new Account();
        account.setId(1L);
        account.setUserId(TEST_USER_ID);
        account.setCurrency("TRY");
        account.setBalance(BigDecimal.ZERO);
        account.setIban("TR123456");

        when(accountService.createAccount(any(AccountCreateRequest.class), eq(TEST_USER_ID))).thenReturn(account);

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
        AccountCreateRequest request = new AccountCreateRequest();
        request.setCurrency("TRY");

        mockMvc.perform(post("/api/v1/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createAccount_WhenAccountServiceThrowsException_ShouldThrowServletException() {
        // Arrange
        AccountCreateRequest request = new AccountCreateRequest();
        request.setCurrency("TRY");

        when(accountService.createAccount(any(AccountCreateRequest.class), eq(TEST_USER_ID)))
                .thenThrow(new RuntimeException("Account creation failed"));

        // Act & Assert
        // WebMvcTest ortamında ExceptionHandler yakalamazsa ServletException fırlatılır.
        Exception exception = assertThrows(ServletException.class, () -> {
            mockMvc.perform(post("/api/v1/accounts")
                    .header(USER_ID_HEADER, TEST_USER_ID)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)));
        });

        assertInstanceOf(RuntimeException.class, exception.getCause());
        assertTrue(exception.getCause().getMessage().contains("Account creation failed"));
    }

    @Test
    void getAccounts_WhenAccountsExist_ShouldReturnAccountList() throws Exception {
        Account account = new Account();
        account.setId(1L);
        account.setUserId(TEST_USER_ID);
        account.setBalance(BigDecimal.TEN);

        when(accountService.getAccountsByUserId(TEST_USER_ID)).thenReturn(List.of(account));

        mockMvc.perform(get("/api/v1/accounts")
                        .header(USER_ID_HEADER, TEST_USER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(1))
                .andExpect(jsonPath("$.message").value("Accounts retrieved successfully."));
    }

    @Test
    void getAccounts_WhenNoAccountsExist_ShouldReturnEmptyList() throws Exception {
        when(accountService.getAccountsByUserId(TEST_USER_ID)).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/v1/accounts")
                        .header(USER_ID_HEADER, TEST_USER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isEmpty())
                .andExpect(jsonPath("$.message").value("Accounts retrieved successfully."));
    }

    @Test
    void getAccounts_WhenUserIdHeaderIsMissing_ShouldReturnBadRequest() throws Exception {
        mockMvc.perform(get("/api/v1/accounts"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deposit_WhenRequestIsValid_ShouldReturnUpdatedAccount() throws Exception {
        String iban = "TR123456";
        DepositRequest request = new DepositRequest();
        request.setAmount(new BigDecimal("100.00"));

        Account account = new Account();
        account.setIban(iban);
        account.setBalance(new BigDecimal("100.00"));

        when(accountService.depositToAccount(eq(iban), any(DepositRequest.class))).thenReturn(account);

        mockMvc.perform(post("/api/v1/accounts/{iban}/deposits", iban)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.balance").value(100.00))
                .andExpect(jsonPath("$.message").value("Deposit successful."));
    }

    @Test
    void deposit_WhenAccountNotFound_ShouldReturnNotFound() throws Exception {
        String iban = "TR_NOT_FOUND";
        DepositRequest request = new DepositRequest();
        request.setAmount(new BigDecimal("50.00"));

        when(accountService.depositToAccount(eq(iban), any(DepositRequest.class)))
                .thenThrow(new AccountNotFoundException("Account not found"));

        mockMvc.perform(post("/api/v1/accounts/{iban}/deposits", iban)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    void deposit_WhenDepositRequestIsInvalid_ShouldReturnBadRequest() throws Exception {
        String iban = "TR123456";
        DepositRequest request = new DepositRequest();
        request.setAmount(new BigDecimal("-10.00"));

        mockMvc.perform(post("/api/v1/accounts/{iban}/deposits", iban)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}