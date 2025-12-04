package com.leonardoramos.notificationservice.events;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrderCreatedEvent {
    private String eventId;
    private String orderId;
    private String userId;
    private double totalAmount;
    private long timestamp;
}
