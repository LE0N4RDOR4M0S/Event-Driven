package com.leonardoramos.paymentservice.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "processed_events")
@AllArgsConstructor
@NoArgsConstructor
public class ProcessedEvent {
    @Id
    private String eventId;
}
