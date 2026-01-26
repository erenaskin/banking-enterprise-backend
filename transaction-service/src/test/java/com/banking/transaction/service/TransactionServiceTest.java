package com.banking.transaction.service;

import com.banking.transaction.dto.TransactionRequest;
import com.banking.transaction.dto.TransferSuccessEvent;
import com.banking.transaction.entity.Account;
import com.banking.transaction.entity.Outbox;
import com.banking.transaction.entity.TransactionLedger;
import com.banking.transaction.exception.IdempotencyException;
import com.banking.transaction.exception.InsufficientFundsException;
import com.banking.transaction.exception.UnauthorizedTransactionException;
import com.banking.transaction.repository.AccountRepository;
import com.banking.transaction.repository.OutboxRepository;
import com.banking.transaction.repository.TransactionLedgerRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private TransactionLedgerRepository transactionLedgerRepository;

    @Mock
    private OutboxRepository outboxRepository;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private TransactionService transactionService;

    @Test
    void executeTransaction_WithSufficientFunds_ShouldTransferMoneyAndSaveOutbox() throws Exception {
        // Arrange
        String correlationId = "corr-123";
        Long userId = 1L;
        String fromIban = "TR111";
        String toIban = "TR222";
        BigDecimal amount = new BigDecimal("100.00");

        TransactionRequest request = new TransactionRequest();
        request.setFromIban(fromIban);
        request.setToIban(toIban);
        request.setAmount(amount);

        Account fromAccount = new Account();
        fromAccount.setIban(fromIban);
        fromAccount.setUserId(userId);
        fromAccount.setBalance(new BigDecimal("200.00"));

        Account toAccount = new Account();
        toAccount.setIban(toIban);
        toAccount.setBalance(new BigDecimal("50.00"));

        when(transactionLedgerRepository.existsByCorrelationIdStartingWith(correlationId)).thenReturn(false);
        when(accountRepository.findByIban(fromIban)).thenReturn(Optional.of(fromAccount));
        when(accountRepository.findByIban(toIban)).thenReturn(Optional.of(toAccount));
        when(objectMapper.writeValueAsString(any(TransferSuccessEvent.class))).thenReturn("json-payload");

        // Act
        transactionService.executeTransaction(request, correlationId, userId);

        // Assert
        assertEquals(new BigDecimal("100.00"), fromAccount.getBalance());
        assertEquals(new BigDecimal("150.00"), toAccount.getBalance());

        verify(accountRepository, times(2)).save(any(Account.class));
        verify(transactionLedgerRepository, times(2)).save(any(TransactionLedger.class));
        verify(outboxRepository).save(any(Outbox.class));
    }

    @Test
    void executeTransaction_WithInsufficientFunds_ShouldThrowException() {
        // Arrange
        String correlationId = "corr-123";
        Long userId = 1L;
        String fromIban = "TR111";
        String toIban = "TR222";
        BigDecimal amount = new BigDecimal("300.00");

        TransactionRequest request = new TransactionRequest();
        request.setFromIban(fromIban);
        request.setToIban(toIban);
        request.setAmount(amount);

        Account fromAccount = new Account();
        fromAccount.setIban(fromIban);
        fromAccount.setUserId(userId);
        fromAccount.setBalance(new BigDecimal("200.00"));

        Account toAccount = new Account();
        toAccount.setIban(toIban);

        when(transactionLedgerRepository.existsByCorrelationIdStartingWith(correlationId)).thenReturn(false);
        when(accountRepository.findByIban(fromIban)).thenReturn(Optional.of(fromAccount));
        when(accountRepository.findByIban(toIban)).thenReturn(Optional.of(toAccount));

        // Act & Assert
        InsufficientFundsException exception = assertThrows(InsufficientFundsException.class, () ->
            transactionService.executeTransaction(request, correlationId, userId)
        );
        assertEquals("Insufficient funds", exception.getMessage());

        verify(accountRepository, never()).save(any(Account.class));
        verify(transactionLedgerRepository, never()).save(any(TransactionLedger.class));
        verify(outboxRepository, never()).save(any(Outbox.class));
    }

    @Test
    void executeTransaction_WithUnauthorizedUser_ShouldThrowException() {
        // Arrange
        String correlationId = "corr-123";
        Long userId = 1L;
        Long otherUserId = 2L;
        String fromIban = "TR111";

        TransactionRequest request = new TransactionRequest();
        request.setFromIban(fromIban);

        Account fromAccount = new Account();
        fromAccount.setIban(fromIban);
        fromAccount.setUserId(otherUserId); // Different user

        when(transactionLedgerRepository.existsByCorrelationIdStartingWith(correlationId)).thenReturn(false);
        when(accountRepository.findByIban(fromIban)).thenReturn(Optional.of(fromAccount));

        // Act & Assert
        assertThrows(UnauthorizedTransactionException.class, () -> 
            transactionService.executeTransaction(request, correlationId, userId)
        );

        verify(accountRepository, never()).save(any(Account.class));
    }

    @Test
    void executeTransaction_WithDuplicateCorrelationId_ShouldThrowIdempotencyException() {
        // Arrange
        String correlationId = "corr-123";
        Long userId = 1L;
        TransactionRequest request = new TransactionRequest();

        when(transactionLedgerRepository.existsByCorrelationIdStartingWith(correlationId)).thenReturn(true);

        // Act & Assert
        assertThrows(IdempotencyException.class, () -> 
            transactionService.executeTransaction(request, correlationId, userId)
        );

        verify(accountRepository, never()).findByIban(any());
    }

    @Test
    void executeTransaction_WhenJsonProcessingExceptionOccurs_ShouldThrowIllegalStateException() throws JsonProcessingException {
        // Arrange
        String correlationId = "corr-123";
        Long userId = 1L;
        String fromIban = "TR111";
        String toIban = "TR222";
        BigDecimal amount = new BigDecimal("100.00");

        TransactionRequest request = new TransactionRequest();
        request.setFromIban(fromIban);
        request.setToIban(toIban);
        request.setAmount(amount);

        Account fromAccount = new Account();
        fromAccount.setIban(fromIban);
        fromAccount.setUserId(userId);
        fromAccount.setBalance(new BigDecimal("200.00"));

        Account toAccount = new Account();
        toAccount.setIban(toIban);
        toAccount.setBalance(new BigDecimal("50.00"));

        when(transactionLedgerRepository.existsByCorrelationIdStartingWith(correlationId)).thenReturn(false);
        when(accountRepository.findByIban(fromIban)).thenReturn(Optional.of(fromAccount));
        when(accountRepository.findByIban(toIban)).thenReturn(Optional.of(toAccount));
        
        // Simulate JsonProcessingException when writing value as string
        when(objectMapper.writeValueAsString(any(TransferSuccessEvent.class))).thenThrow(new JsonProcessingException("Test JSON processing error") {});

        // Act & Assert
        IllegalStateException exception = assertThrows(IllegalStateException.class, () ->
            transactionService.executeTransaction(request, correlationId, userId)
        );
        assertEquals("Failed to serialize event payload", exception.getMessage());
        assertTrue(exception.getCause() instanceof JsonProcessingException);

        verify(accountRepository, times(2)).save(any(Account.class)); // Hesaplar güncellenmeli
        verify(transactionLedgerRepository, times(2)).save(any(TransactionLedger.class)); // Ledger kayıtları atılmalı
        verify(outboxRepository, never()).save(any(Outbox.class)); // Outbox'a kaydedilmemeli
    }
}