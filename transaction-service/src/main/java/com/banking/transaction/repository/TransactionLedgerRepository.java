package com.banking.transaction.repository;

import com.banking.transaction.entity.TransactionLedger;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TransactionLedgerRepository extends JpaRepository<TransactionLedger, Long> {
    Optional<TransactionLedger> findByCorrelationId(String correlationId);
    boolean existsByCorrelationIdStartingWith(String correlationId);
}
