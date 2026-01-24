package com.banking.notification.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TransferSuccessEvent {
    private Long userId;
    private BigDecimal amount;
    private String toIban;
}