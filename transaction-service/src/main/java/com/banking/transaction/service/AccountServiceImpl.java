package com.banking.transaction.service;

import com.banking.transaction.dto.AccountCreateRequest;
import com.banking.transaction.dto.DepositRequest;
import com.banking.transaction.entity.Account;
import com.banking.transaction.repository.AccountRepository;
import com.banking.transaction.exception.AccountNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Random;

@Service
public class AccountServiceImpl implements AccountService {

    @Autowired
    private AccountRepository accountRepository;

    private static final Random random = new Random();

    @Override
    public Account createAccount(AccountCreateRequest request, Long userId) {
        Account account = new Account();
        account.setUserId(userId);
        account.setCurrency(request.getCurrency());
        account.setBalance(BigDecimal.ZERO);
        account.setIban(generateIban());
        return accountRepository.save(account);
    }

    @Override
    public List<Account> getAccountsByUserId(Long userId) {
        return accountRepository.findAllByUserId(userId);
    }

    @Transactional
    @Override
    public Account depositToAccount(String iban, DepositRequest request) {
        Account account = accountRepository.findByIban(iban)
                .orElseThrow(() -> new AccountNotFoundException("Account not found with IBAN: " + iban));
        account.setBalance(account.getBalance().add(request.getAmount()));
        return accountRepository.save(account);
    }

    private String generateIban() {
        // Generate a more realistic Turkish IBAN (TR + 24 digits)
        StringBuilder iban = new StringBuilder("TR");
        for (int i = 0; i < 24; i++) {
            iban.append(random.nextInt(10));
        }
        return iban.toString();
    }
}
