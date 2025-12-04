package com.leonardoramos.notificationservice.repository;

import com.leonardoramos.notificationservice.model.ProcessedEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ProcessedEventRepository extends JpaRepository<ProcessedEvent, String> {
}
