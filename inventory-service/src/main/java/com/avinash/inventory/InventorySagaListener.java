package com.avinash.inventory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class InventorySagaListener {

    private static final Logger log = LoggerFactory.getLogger(InventorySagaListener.class);

    private final InventoryRepository inventoryRepository;
    private final ProcessedEventRepository processedEventRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public InventorySagaListener(InventoryRepository inventoryRepository,
                                 ProcessedEventRepository processedEventRepository,
                                 KafkaTemplate<String, String> kafkaTemplate,
                                 ObjectMapper objectMapper) {
        this.inventoryRepository = inventoryRepository;
        this.processedEventRepository = processedEventRepository;
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "order-events", groupId = "inventory-group")
    @Transactional
    public void onOrderCreated(String payload) {
        try {
            JsonNode node = objectMapper.readTree(payload);
            String eventId = node.get("eventId").asText();
            String orderId = node.get("orderId").asText();
            String productId = node.get("productId").asText();
            String eventType = node.has("eventType") ? node.get("eventType").asText() : "";
            
            // The topic might have different event types, skip if not OrderCreated
            if (node.has("eventType") && !eventType.equals("OrderCreatedEvent")) {
                return;
            }

            if (processedEventRepository.existsById(eventId)) {
                log.info("Event {} already processed", eventId);
                return;
            }

            log.info("Processing order {} for product {}", orderId, productId);
            
            boolean success = false;
            Inventory inventory = inventoryRepository.findById(productId).orElse(null);
            
            if (inventory != null && inventory.getStock() > 0) {
                inventory.setStock(inventory.getStock() - 1);
                inventoryRepository.save(inventory);
                success = true;
                log.info("Reserved stock for order {}", orderId);
            } else {
                log.warn("Insufficient stock or product not found for order {}", orderId);
            }

            processedEventRepository.save(new ProcessedEvent(eventId));

            String nextTopic = success ? "inventory-reserved" : "inventory-failed";
            kafkaTemplate.send(nextTopic, orderId, payload);

        } catch (Exception e) {
            log.error("Failed to process order-events", e);
            throw new RuntimeException(e);
        }
    }

    @KafkaListener(topics = {"payment-failed", "fulfillment-failed"}, groupId = "inventory-group")
    @Transactional
    public void onPaymentFailed(String payload) {
        try {
            JsonNode node = objectMapper.readTree(payload);
            String eventId = node.get("eventId").asText();
            String orderId = node.get("orderId").asText();
            String productId = node.get("productId").asText();

            if (processedEventRepository.existsById(eventId)) {
                log.info("Compensation event {} already processed", eventId);
                return;
            }

            log.info("Compensating stock for order {} and product {}", orderId, productId);
            inventoryRepository.findById(productId).ifPresent(inventory -> {
                inventory.setStock(inventory.getStock() + 1);
                inventoryRepository.save(inventory);
            });

            processedEventRepository.save(new ProcessedEvent(eventId));
        } catch (Exception e) {
            log.error("Failed to process payment-failed", e);
            throw new RuntimeException(e);
        }
    }
}
