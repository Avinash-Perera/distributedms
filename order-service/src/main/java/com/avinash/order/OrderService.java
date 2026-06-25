package com.avinash.order;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    public OrderService(OrderRepository orderRepository, OutboxEventRepository outboxEventRepository, ObjectMapper objectMapper) {
        this.orderRepository = orderRepository;
        this.outboxEventRepository = outboxEventRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public Order createOrder(CreateOrderRequest request) {
        String orderId = "ORD-" + UUID.randomUUID().toString();
        
        Order order = new Order(
                orderId,
                request.customerId(),
                request.customerEmail(),
                request.productId(),
                "PENDING"
        );
        orderRepository.save(order);

        String eventId = UUID.randomUUID().toString();
        OrderCreatedEvent event = new OrderCreatedEvent(
                eventId,
                orderId,
                request.customerId(),
                request.customerEmail(),
                request.productId(),
                "PENDING"
        );

        try {
            String payload = objectMapper.writeValueAsString(event);
            OutboxEvent outboxEvent = new OutboxEvent(
                    eventId,
                    orderId,
                    "OrderCreatedEvent",
                    payload
            );
            outboxEventRepository.save(outboxEvent);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize OrderCreatedEvent", e);
        }

        return order;
    }
}
