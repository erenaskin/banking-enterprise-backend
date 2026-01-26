package com.banking.transaction.controller;

import com.banking.transaction.dto.AccountCreateRequest;
import com.banking.transaction.dto.DepositRequest;
import com.banking.transaction.entity.Account;
import com.banking.transaction.service.AccountService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
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

    @Test
    void createAccount_ShouldReturnCreatedAccount() throws Exception {
        Long userId = 1L;
        AccountCreateRequest request = new AccountCreateRequest();
        request.setCurrency("TRY");

        Account account = new Account();
        account.setId(1L);
        account.setUserId(userId);
        account.setCurrency("TRY");
        account.setBalance(BigDecimal.ZERO);
        account.setIban("TR123456");

        when(accountService.createAccount(any(AccountCreateRequest.class), eq(userId))).thenReturn(account);

        mockMvc.perform(post("/api/v1/accounts")
                        .header("X-User-Id", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.currency").value("TRY"))
                .andExpect(jsonPath("$.message").value("Account created successfully."));
    }

    @Test
    void getAccounts_ShouldReturnAccountList() throws Exception {
        Long userId = 1L;
        Account account = new Account();
        account.setId(1L);
        account.setUserId(userId);
        account.setBalance(BigDecimal.TEN);

        when(accountService.getAccountsByUserId(userId)).thenReturn(List.of(account));

        mockMvc.perform(get("/api/v1/accounts")
                        .header("X-User-Id", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(1))
                .andExpect(jsonPath("$.message").value("Accounts retrieved successfully."));
    }

    @Test
    void deposit_ShouldReturnUpdatedAccount() throws Exception {
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
}