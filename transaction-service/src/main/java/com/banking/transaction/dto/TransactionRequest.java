package com.banking.transaction.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class TransactionRequest {

    @NotBlank
    @Pattern(regexp = "TR[0-9]{24}")
    private String fromIban;

    @NotBlank
    @Pattern(regexp = "TR[0-9]{24}")
    private String toIban;

    @NotNull
    @Positive
    private BigDecimal amount;
}