package com.leonardoramos.orderservice.dto;

public record CreateOrderRequest(String eventId, String orderId, String userId, double totalAmount, long timestamp) {
}