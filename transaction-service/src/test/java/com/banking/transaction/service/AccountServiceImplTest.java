package com.banking.transaction.service;

import com.banking.transaction.dto.AccountCreateRequest;
import com.banking.transaction.dto.DepositRequest;
import com.banking.transaction.entity.Account;
import com.banking.transaction.exception.AccountNotFoundException;
import com.banking.transaction.repository.AccountRepository;
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
class AccountServiceImplTest {

    @Mock
    private AccountRepository accountRepository;

    @InjectMocks
    private AccountServiceImpl accountService;

    @Test
    void createAccount_ShouldCreateNewAccountWithZeroBalance() {
        // Arrange
        Long userId = 1L;
        AccountCreateRequest request = new AccountCreateRequest();
        request.setCurrency("TRY");

        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        Account createdAccount = accountService.createAccount(request, userId);

        // Assert
        assertNotNull(createdAccount);
        assertEquals(userId, createdAccount.getUserId());
        assertEquals("TRY", createdAccount.getCurrency());
        assertEquals(BigDecimal.ZERO, createdAccount.getBalance());
        assertNotNull(createdAccount.getIban());
        assertTrue(createdAccount.getIban().startsWith("TR"));
        verify(accountRepository).save(any(Account.class));
    }

    @Test
    void depositToAccount_WithValidIban_ShouldIncreaseBalance() {
        // Arrange
        String iban = "TR123456789012345678901234";
        BigDecimal initialBalance = new BigDecimal("100.00");
        BigDecimal depositAmount = new BigDecimal("50.00");

        Account account = new Account();
        account.setIban(iban);
        account.setBalance(initialBalance);

        DepositRequest request = new DepositRequest();
        request.setAmount(depositAmount);

        when(accountRepository.findByIban(iban)).thenReturn(Optional.of(account));
        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        Account updatedAccount = accountService.depositToAccount(iban, request);

        // Assert
        assertEquals(initialBalance.add(depositAmount), updatedAccount.getBalance());
        verify(accountRepository).findByIban(iban);
        verify(accountRepository).save(account);
    }

    @Test
    void depositToAccount_WithInvalidIban_ShouldThrowException() {
        // Arrange
        String iban = "TR_INVALID";
        DepositRequest request = new DepositRequest();
        request.setAmount(new BigDecimal("50.00"));

        when(accountRepository.findByIban(iban)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(AccountNotFoundException.class, () -> accountService.depositToAccount(iban, request));
        verify(accountRepository).findByIban(iban);
        verify(accountRepository, never()).save(any());
    }
}