package com.banking.transaction.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.FORBIDDEN)
public class UnauthorizedTransactionException extends RuntimeException {
    public UnauthorizedTransactionException(String message) {
        super(message);
    }
}
