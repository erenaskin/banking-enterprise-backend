package com.banking.notification.listener;

import com.banking.notification.dto.TransferSuccessEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class NotificationListener {

    @Autowired
    private ObjectMapper objectMapper;

    @KafkaListener(topics = "transaction-events", groupId = "notification-group")
    public void handleNotification(String message) {
        try {
            TransferSuccessEvent event = objectMapper.readValue(message, TransferSuccessEvent.class);
            System.out.println("Sayın " + event.getUserId() + ", " + event.getAmount() + " tutarındaki işleminiz gerçekleşmiştir");
        } catch (Exception e) {
            System.err.println("Failed to process message: " + message);
            e.printStackTrace();
        }
    }
}