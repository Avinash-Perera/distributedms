package com.avinash.order;

public record OrderCreatedEvent(String eventId, String orderId, String customerId, String customerEmail, String productId, String status) {
}
