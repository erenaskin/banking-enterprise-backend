package com.banking.transaction.service;

import com.banking.transaction.dto.AccountCreateRequest;
import com.banking.transaction.dto.DepositRequest;
import com.banking.transaction.entity.Account;

import java.util.List;

public interface AccountService {
    Account createAccount(AccountCreateRequest request, Long userId);
    List<Account> getAccountsByUserId(Long userId);
    Account depositToAccount(String iban, DepositRequest request);
}
