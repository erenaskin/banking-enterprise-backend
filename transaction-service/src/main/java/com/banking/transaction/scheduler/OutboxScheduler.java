package com.banking.transaction.scheduler;

import com.banking.transaction.entity.Outbox;
import com.banking.transaction.repository.OutboxRepository;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Component
public class OutboxScheduler {

    private final OutboxRepository outboxRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    public OutboxScheduler(OutboxRepository outboxRepository, KafkaTemplate<String, String> kafkaTemplate) {
        this.outboxRepository = outboxRepository;
        this.kafkaTemplate = kafkaTemplate;
    }

    @Scheduled(fixedDelay = 10000) // 10 seconds
    @Transactional
    public synchronized void processOutbox() {
        List<Outbox> unsentMessages = outboxRepository.findBySentFalse();
        for (Outbox message : unsentMessages) {
            kafkaTemplate.send(message.getTopic(), message.getPayload())
                .thenRun(() -> {
                    message.setSent(true);
                    outboxRepository.save(message);
                })
                .exceptionally(ex -> null);
        }
    }
}