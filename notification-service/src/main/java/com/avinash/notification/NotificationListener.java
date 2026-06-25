package com.avinash.notification;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class NotificationListener {

    private static final Logger log = LoggerFactory.getLogger(NotificationListener.class);
    private final ObjectMapper objectMapper;

    public NotificationListener(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = {"order-shipped", "payment-failed", "inventory-failed", "fulfillment-failed"}, groupId = "notification-group")
    public void onTerminalEvent(String payload) {
        try {
            JsonNode node = objectMapper.readTree(payload);
            String orderId = node.has("orderId") ? node.get("orderId").asText() : "UNKNOWN";
            String email = node.has("customerEmail") ? node.get("customerEmail").asText() : "UNKNOWN";
            
            // Note: Since this service is stateless, idempotency would usually be handled 
            // by a caching layer (like Redis) or checking a third-party email provider's API.
            // For this PoC, we just log it.
            
            log.info("Sending mock email to {} for order {}. Payload: {}", email, orderId, payload);
        } catch (Exception e) {
            log.error("Failed to process notification event", e);
            throw new RuntimeException(e);
        }
    }
}
