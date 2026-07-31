package com.avinash.notification;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

@ExtendWith(MockitoExtension.class)
class NotificationListenerTest {

    private ObjectMapper objectMapper;
    private NotificationListener listener;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        listener = new NotificationListener(objectMapper);
    }

    @Test
    void testOnTerminalEvent() {
        String payload = "{\"eventId\":\"evt-1\", \"orderId\":\"ord-1\", \"customerEmail\":\"test@test.com\"}";
        
        // Notification service only logs currently, so we just assert no exception is thrown
        assertDoesNotThrow(() -> listener.onTerminalEvent(payload));
    }
}
