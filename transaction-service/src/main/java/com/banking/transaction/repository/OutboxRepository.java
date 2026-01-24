package com.banking.transaction.repository;

import com.banking.transaction.entity.Outbox;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OutboxRepository extends JpaRepository<Outbox, Long> {
    List<Outbox> findBySentFalse();
}