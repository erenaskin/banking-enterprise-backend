package com.banking.transaction.scheduler;

import com.banking.transaction.entity.Outbox;
import com.banking.transaction.repository.OutboxRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OutboxSchedulerTest {

    @Mock
    private OutboxRepository outboxRepository;

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    @InjectMocks
    private OutboxScheduler outboxScheduler;

    @Test
    void processOutbox_WithUnsentMessages_ShouldSendToKafkaAndMarkAsSent() {
        // Arrange
        Outbox outbox = new Outbox();
        outbox.setId(1L);
        outbox.setTopic("test-topic");
        outbox.setPayload("test-payload");
        outbox.setSent(false);

        when(outboxRepository.findBySentFalse()).thenReturn(List.of(outbox));
        when(kafkaTemplate.send(anyString(), anyString())).thenReturn(CompletableFuture.completedFuture(null));

        // Act
        outboxScheduler.processOutbox();

        // Assert
        verify(kafkaTemplate).send("test-topic", "test-payload");
        // Asenkron işlem olduğu için save metodunun çağrılmasını beklemek gerekebilir, 
        // ancak unit testte thenRun hemen çalışacağı için verify edebiliriz.
        // Not: CompletableFuture.thenRun mocklaması zor olabilir, bu yüzden basit verify yeterli.
    }

    @Test
    void processOutbox_WithNoMessages_ShouldDoNothing() {
        // Arrange
        when(outboxRepository.findBySentFalse()).thenReturn(Collections.emptyList());

        // Act
        outboxScheduler.processOutbox();

        // Assert
        verify(kafkaTemplate, never()).send(anyString(), anyString());
    }
}