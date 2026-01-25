package com.banking.transaction.service;

import com.banking.transaction.dto.TransactionRequest;
import com.banking.transaction.dto.TransferSuccessEvent;
import com.banking.transaction.entity.Account;
import com.banking.transaction.entity.Outbox;
import com.banking.transaction.entity.TransactionLedger;
import com.banking.transaction.entity.TransactionType;
import com.banking.transaction.exception.AccountNotFoundException;
import com.banking.transaction.exception.IdempotencyException;
import com.banking.transaction.exception.InsufficientFundsException;
import com.banking.transaction.exception.UnauthorizedTransactionException;
import com.banking.transaction.repository.AccountRepository;
import com.banking.transaction.repository.OutboxRepository;
import com.banking.transaction.repository.TransactionLedgerRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class TransactionService {

    private final AccountRepository accountRepository;
    private final TransactionLedgerRepository transactionLedgerRepository;
    private final OutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;

    public TransactionService(AccountRepository accountRepository,
                              TransactionLedgerRepository transactionLedgerRepository,
                              OutboxRepository outboxRepository,
                              ObjectMapper objectMapper) {
        this.accountRepository = accountRepository;
        this.transactionLedgerRepository = transactionLedgerRepository;
        this.outboxRepository = outboxRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public void executeTransaction(TransactionRequest request, String correlationId, Long userId) {
        if (transactionLedgerRepository.existsByCorrelationIdStartingWith(correlationId)) {
            throw new IdempotencyException("Transaction with Correlation-ID " + correlationId + " has already been processed.");
        }

        Account fromAccount = accountRepository.findByIban(request.getFromIban())
                .orElseThrow(() -> new AccountNotFoundException("Sender account not found"));

        if (!fromAccount.getUserId().equals(userId)) {
            throw new UnauthorizedTransactionException("User is not the owner of the source account.");
        }

        Account toAccount = accountRepository.findByIban(request.getToIban())
                .orElseThrow(() -> new AccountNotFoundException("Receiver account not found"));

        if (fromAccount.getBalance().compareTo(request.getAmount()) < 0) {
            throw new InsufficientFundsException("Insufficient funds");
        }

        fromAccount.setBalance(fromAccount.getBalance().subtract(request.getAmount()));
        toAccount.setBalance(toAccount.getBalance().add(request.getAmount()));

        accountRepository.save(fromAccount);
        accountRepository.save(toAccount);

        TransactionLedger debitLedger = TransactionLedger.builder()
                .account(fromAccount)
                .amount(request.getAmount().negate())
                .transactionType(TransactionType.DEBIT)
                .transactionDate(LocalDateTime.now())
                .correlationId(correlationId + "-D")
                .build();

        TransactionLedger creditLedger = TransactionLedger.builder()
                .account(toAccount)
                .amount(request.getAmount())
                .transactionType(TransactionType.CREDIT)
                .transactionDate(LocalDateTime.now())
                .correlationId(correlationId + "-C")
                .build();

        transactionLedgerRepository.save(debitLedger);
        transactionLedgerRepository.save(creditLedger);

        TransferSuccessEvent event = new TransferSuccessEvent(fromAccount.getUserId(), request.getAmount(), request.getToIban());
        try {
            Outbox outbox = Outbox.builder()
                    .topic("transaction-events")
                    .payload(objectMapper.writeValueAsString(event))
                    .createdAt(LocalDateTime.now())
                    .sent(false)
                    .build();
            outboxRepository.save(outbox);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize event payload", e);
        }
    }
}