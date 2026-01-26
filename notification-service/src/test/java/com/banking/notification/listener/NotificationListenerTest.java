package com.banking.notification.listener;

import com.banking.notification.dto.TransferSuccessEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationListenerTest {

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private NotificationListener notificationListener;

    @Test
    void handleNotification_WithValidMessage_ShouldProcessSuccessfully() throws Exception {
        // Arrange
        String message = "{\"userId\":1,\"amount\":100.0,\"targetIban\":\"TR123\"}";
        TransferSuccessEvent event = new TransferSuccessEvent(1L, new BigDecimal("100.0"), "TR123");

        when(objectMapper.readValue(message, TransferSuccessEvent.class)).thenReturn(event);

        // Act
        notificationListener.handleNotification(message);

        // Assert
        verify(objectMapper).readValue(message, TransferSuccessEvent.class);
    }

    @Test
    void handleNotification_WithInvalidMessage_ShouldLogErrorAndNotThrowException() throws Exception {
        // Arrange
        String invalidMessage = "invalid-json";
        when(objectMapper.readValue(invalidMessage, TransferSuccessEvent.class)).thenThrow(new RuntimeException("Json parse error"));

        // Act
        notificationListener.handleNotification(invalidMessage);

        // Assert
        verify(objectMapper).readValue(invalidMessage, TransferSuccessEvent.class);
        // Exception yutulduğu için test başarılı olmalı
    }
}