package com.avinash.payment;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class PaymentSagaListener {

    private static final Logger log = LoggerFactory.getLogger(PaymentSagaListener.class);

    private final ProcessedEventRepository processedEventRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public PaymentSagaListener(ProcessedEventRepository processedEventRepository,
                               KafkaTemplate<String, String> kafkaTemplate,
                               ObjectMapper objectMapper) {
        this.processedEventRepository = processedEventRepository;
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "inventory-reserved", groupId = "payment-group")
    @Transactional
    public void onInventoryReserved(String payload) {
        try {
            JsonNode node = objectMapper.readTree(payload);
            String eventId = node.get("eventId").asText();
            String orderId = node.get("orderId").asText();

            if (processedEventRepository.existsById(eventId)) {
                log.info("Event {} already processed", eventId);
                return;
            }

            String customerId = node.has("customerId") ? node.get("customerId").asText() : "";
            log.info("Processing payment for order {} (customer: {})", orderId, customerId);
            
            // Mock payment logic: fail if customerId is POOR-GUY
            boolean success = !"POOR-GUY".equals(customerId); 

            processedEventRepository.save(new ProcessedEvent(eventId));

            String nextTopic = success ? "payment-succeeded" : "payment-failed";
            log.info("Payment {}, publishing to {}", success ? "successful" : "failed", nextTopic);
            kafkaTemplate.send(nextTopic, orderId, payload);

        } catch (Exception e) {
            log.error("Failed to process inventory-reserved", e);
            throw new RuntimeException(e);
        }
    }

    @KafkaListener(topics = "fulfillment-failed", groupId = "payment-group")
    @Transactional
    public void onFulfillmentFailed(String payload) {
        try {
            JsonNode node = objectMapper.readTree(payload);
            String eventId = node.get("eventId").asText();
            String orderId = node.get("orderId").asText();

            if (processedEventRepository.existsById(eventId)) {
                log.info("Compensation event {} already processed", eventId);
                return;
            }

            log.info("Compensating payment (refunding) for order {}", orderId);
            // Mock refund logic

            processedEventRepository.save(new ProcessedEvent(eventId));
        } catch (Exception e) {
            log.error("Failed to process fulfillment-failed", e);
            throw new RuntimeException(e);
        }
    }
}
