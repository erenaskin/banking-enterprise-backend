package com.banking.transaction.controller;

import com.banking.transaction.dto.TransactionRequest;
import com.banking.transaction.exception.AccountNotFoundException;
import com.banking.transaction.exception.IdempotencyException;
import com.banking.transaction.exception.InsufficientFundsException;
import com.banking.transaction.exception.UnauthorizedTransactionException;
import com.banking.transaction.service.TransactionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.doNothing;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TransactionController.class)
class TransactionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TransactionService transactionService;

    @Autowired
    private ObjectMapper objectMapper;

    private final String CORRELATION_ID_HEADER = "X-Correlation-ID";
    private final String USER_ID_HEADER = "X-User-Id";
    private final String TEST_CORRELATION_ID = "corr-123";
    private final Long TEST_USER_ID = 1L;

    private TransactionRequest createValidTransactionRequest() {
        TransactionRequest request = new TransactionRequest();
        request.setFromIban("TR123456789012345678901234");
        request.setToIban("TR987654321098765432109876");
        request.setAmount(new BigDecimal("100.00"));
        return request;
    }

    @Test
    void createTransaction_WhenRequestIsValid_ShouldReturnAccepted() throws Exception {
        TransactionRequest request = createValidTransactionRequest();
        doNothing().when(transactionService).executeTransaction(any(TransactionRequest.class), eq(TEST_CORRELATION_ID), eq(TEST_USER_ID));

        mockMvc.perform(post("/api/v1/transactions")
                        .header(USER_ID_HEADER, TEST_USER_ID)
                        .header(CORRELATION_ID_HEADER, TEST_CORRELATION_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.message").value("Transaction accepted for processing."))
                .andExpect(jsonPath("$.data.correlationId").value(TEST_CORRELATION_ID));
    }

    @Test
    void createTransaction_WhenCorrelationIdHeaderIsMissing_ShouldReturnBadRequest() throws Exception {
        TransactionRequest request = createValidTransactionRequest();

        mockMvc.perform(post("/api/v1/transactions")
                        .header(USER_ID_HEADER, TEST_USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createTransaction_WhenUserIdHeaderIsMissing_ShouldReturnBadRequest() throws Exception {
        TransactionRequest request = createValidTransactionRequest();

        mockMvc.perform(post("/api/v1/transactions")
                        .header(CORRELATION_ID_HEADER, TEST_CORRELATION_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createTransaction_WhenTransactionRequestIsInvalid_ShouldReturnBadRequest() throws Exception {
        TransactionRequest request = new TransactionRequest();
        request.setFromIban("INVALID");
        request.setToIban("INVALID");
        request.setAmount(BigDecimal.ZERO);

        mockMvc.perform(post("/api/v1/transactions")
                        .header(USER_ID_HEADER, TEST_USER_ID)
                        .header(CORRELATION_ID_HEADER, TEST_CORRELATION_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createTransaction_WhenAccountNotFoundException_ShouldReturnNotFound() throws Exception {
        TransactionRequest request = createValidTransactionRequest();
        doThrow(new AccountNotFoundException("Account not found")).when(transactionService)
                .executeTransaction(any(TransactionRequest.class), eq(TEST_CORRELATION_ID), eq(TEST_USER_ID));

        mockMvc.perform(post("/api/v1/transactions")
                        .header(USER_ID_HEADER, TEST_USER_ID)
                        .header(CORRELATION_ID_HEADER, TEST_CORRELATION_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    void createTransaction_WhenInsufficientFundsException_ShouldReturnBadRequest() throws Exception {
        TransactionRequest request = createValidTransactionRequest();
        doThrow(new InsufficientFundsException("Insufficient funds")).when(transactionService)
                .executeTransaction(any(TransactionRequest.class), eq(TEST_CORRELATION_ID), eq(TEST_USER_ID));

        mockMvc.perform(post("/api/v1/transactions")
                        .header(USER_ID_HEADER, TEST_USER_ID)
                        .header(CORRELATION_ID_HEADER, TEST_CORRELATION_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createTransaction_WhenUnauthorizedTransactionException_ShouldReturnForbidden() throws Exception {
        // Fix: isUnauthorized() (401) yerine isForbidden() (403) beklendiği için güncellendi.
        TransactionRequest request = createValidTransactionRequest();
        doThrow(new UnauthorizedTransactionException("Unauthorized")).when(transactionService)
                .executeTransaction(any(TransactionRequest.class), eq(TEST_CORRELATION_ID), eq(TEST_USER_ID));

        mockMvc.perform(post("/api/v1/transactions")
                        .header(USER_ID_HEADER, TEST_USER_ID)
                        .header(CORRELATION_ID_HEADER, TEST_CORRELATION_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void createTransaction_WhenIdempotencyException_ShouldReturnConflict() throws Exception {
        TransactionRequest request = createValidTransactionRequest();
        doThrow(new IdempotencyException("Already processed")).when(transactionService)
                .executeTransaction(any(TransactionRequest.class), eq(TEST_CORRELATION_ID), eq(TEST_USER_ID));

        mockMvc.perform(post("/api/v1/transactions")
                        .header(USER_ID_HEADER, TEST_USER_ID)
                        .header(CORRELATION_ID_HEADER, TEST_CORRELATION_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    void createTransaction_WhenGenericException_ShouldThrowServletException() {
        // Fix: GlobalExceptionHandler test ortamında olmadığı için 500 dönmez, exception fırlatır.
        TransactionRequest request = createValidTransactionRequest();
        doThrow(new RuntimeException("Something went wrong")).when(transactionService)
                .executeTransaction(any(TransactionRequest.class), eq(TEST_CORRELATION_ID), eq(TEST_USER_ID));

        Exception exception = assertThrows(ServletException.class, () -> {
            mockMvc.perform(post("/api/v1/transactions")
                    .header(USER_ID_HEADER, TEST_USER_ID)
                    .header(CORRELATION_ID_HEADER, TEST_CORRELATION_ID)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)));
        });

        assertInstanceOf(RuntimeException.class, exception.getCause());
        assertTrue(exception.getCause().getMessage().contains("Something went wrong"));
    }
}