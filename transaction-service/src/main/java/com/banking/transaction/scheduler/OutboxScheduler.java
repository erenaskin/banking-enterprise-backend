package com.banking.transaction.scheduler;

import com.banking.transaction.entity.Outbox;
import com.banking.transaction.repository.OutboxRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Component
public class OutboxScheduler {

    @Autowired
    private OutboxRepository outboxRepository;

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Scheduled(fixedDelay = 10000) // 10 seconds
    @Transactional
    public synchronized void processOutbox() {
        List<Outbox> unsentMessages = outboxRepository.findBySentFalse();
        for (Outbox message : unsentMessages) {
            CompletableFuture<Void> future = kafkaTemplate.send(message.getTopic(), message.getPayload())
                .thenRun(() -> {
                    message.setSent(true);
                    outboxRepository.save(message);
                })
                .exceptionally(ex -> {
                    // Handle the exception, e.g., log it
                    return null;
                });
        }
    }
}