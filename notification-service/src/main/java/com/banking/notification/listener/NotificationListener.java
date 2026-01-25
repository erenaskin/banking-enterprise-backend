package com.banking.notification.listener;

import com.banking.notification.dto.TransferSuccessEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class NotificationListener {

    private static final Logger logger = LoggerFactory.getLogger(NotificationListener.class);
    private final ObjectMapper objectMapper;

    public NotificationListener(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "transaction-events", groupId = "notification-group")
    public void handleNotification(String message) {
        try {
            TransferSuccessEvent event = objectMapper.readValue(message, TransferSuccessEvent.class);
            logger.info("Sayın {}, {} tutarındaki işleminiz gerçekleşmiştir", event.getUserId(), event.getAmount());
        } catch (Exception e) {
            logger.error("Failed to process message: {}", message, e);
        }
    }
}