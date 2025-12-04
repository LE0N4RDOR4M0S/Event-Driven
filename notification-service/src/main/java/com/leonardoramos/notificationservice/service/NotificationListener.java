package com.leonardoramos.notificationservice.service;

import com.leonardoramos.notificationservice.events.OrderCreatedEvent;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Service;

@Service
public class NotificationListener {

    @KafkaListener(
            topics = "orders",
            groupId = "notification-group",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleOrderEvent(OrderCreatedEvent event,
                                 @Header(KafkaHeaders.RECEIVED_KEY) String key) {

        System.out.println("Pagamento recebido para (Chave: " + key + "): " + event.getOrderId());

        try {
            // lógica de negócios (chamar API de gateway, salvar no DB)...
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        System.out.println("Pagamento processado para: " + event.getOrderId());
    }
}