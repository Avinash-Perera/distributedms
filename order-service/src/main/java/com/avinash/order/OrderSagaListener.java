package com.avinash.order;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class OrderSagaListener {

    private static final Logger log = LoggerFactory.getLogger(OrderSagaListener.class);
    
    private final OrderRepository orderRepository;
    private final ProcessedEventRepository processedEventRepository;
    private final ObjectMapper objectMapper;

    public OrderSagaListener(OrderRepository orderRepository, ProcessedEventRepository processedEventRepository, ObjectMapper objectMapper) {
        this.orderRepository = orderRepository;
        this.processedEventRepository = processedEventRepository;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = {"payment-failed", "inventory-failed", "fulfillment-failed"}, groupId = "order-group")
    @Transactional
    public void handleFailureEvents(String payload) {
        try {
            JsonNode node = objectMapper.readTree(payload);
            String eventId = node.has("eventId") ? node.get("eventId").asText() : null;
            String orderId = node.has("orderId") ? node.get("orderId").asText() : null;
            
            if (eventId == null || orderId == null) {
                log.warn("Received event without eventId or orderId: {}", payload);
                return;
            }

            // Idempotency check
            if (processedEventRepository.existsById(eventId)) {
                log.info("Event {} already processed, skipping.", eventId);
                return;
            }

            log.info("Processing failure event for order {}. Marking as CANCELLED.", orderId);
            orderRepository.findById(orderId).ifPresent(order -> {
                order.setStatus("CANCELLED");
                orderRepository.save(order);
            });

            // Mark as processed
            processedEventRepository.save(new ProcessedEvent(eventId));

        } catch (Exception e) {
            log.error("Error processing failure event: {}", payload, e);
            throw new RuntimeException("Failed to process event", e); // Will be retried and sent to DLT by Spring Kafka Error Handler if configured
        }
    }
}
