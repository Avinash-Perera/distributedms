package com.avinash.order;

public record CreateOrderRequest(String customerId, String customerEmail, String productId) {
}
