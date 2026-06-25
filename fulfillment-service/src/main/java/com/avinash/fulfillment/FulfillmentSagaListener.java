package com.avinash.fulfillment;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class FulfillmentSagaListener {

    private static final Logger log = LoggerFactory.getLogger(FulfillmentSagaListener.class);

    private final ProcessedEventRepository processedEventRepository;
    private final ShipmentRepository shipmentRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public FulfillmentSagaListener(ProcessedEventRepository processedEventRepository,
                                   ShipmentRepository shipmentRepository,
                                   KafkaTemplate<String, String> kafkaTemplate,
                                   ObjectMapper objectMapper) {
        this.processedEventRepository = processedEventRepository;
        this.shipmentRepository = shipmentRepository;
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "payment-succeeded", groupId = "fulfillment-group")
    @Transactional
    public void onPaymentSucceeded(String payload) {
        try {
            JsonNode node = objectMapper.readTree(payload);
            String eventId = node.get("eventId").asText();
            String orderId = node.get("orderId").asText();

            String customerId = node.has("customerId") ? node.get("customerId").asText() : "";
            
            if (processedEventRepository.existsById(eventId)) {
                log.info("Event {} already processed", eventId);
                return;
            }

            log.info("Processing fulfillment for order {} (customer: {})", orderId, customerId);
            
            boolean success = !"BAD-ADDRESS".equals(customerId);

            if (success) {
                Shipment shipment = new Shipment(orderId, "SHIPPED");
                shipmentRepository.save(shipment);
            } else {
                log.warn("Fulfillment failed due to invalid address for order {}", orderId);
            }

            processedEventRepository.save(new ProcessedEvent(eventId));

            String nextTopic = success ? "order-shipped" : "fulfillment-failed";
            log.info("Fulfillment {}, publishing to {}", success ? "successful" : "failed", nextTopic);
            kafkaTemplate.send(nextTopic, orderId, payload);

        } catch (Exception e) {
            log.error("Failed to process payment-succeeded", e);
            throw new RuntimeException(e);
        }
    }
}
