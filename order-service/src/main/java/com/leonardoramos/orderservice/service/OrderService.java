package com.leonardoramos.orderservice.service;

import com.leonardoramos.orderservice.dto.CreateOrderRequest;
import com.leonardoramos.orderservice.events.OrderCreatedEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Service
public class OrderService {
    private final KafkaTemplate<String, OrderCreatedEvent> kafkaTemplate;
    private final String TOPIC = "orders";

    @Autowired
    public OrderService(KafkaTemplate<String, OrderCreatedEvent> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void createOrder(CreateOrderRequest request) {
        String orderId = "ORD-" + UUID.randomUUID().toString().substring(0, 8);

        OrderCreatedEvent event = new OrderCreatedEvent(
                UUID.randomUUID().toString(),
                orderId,
                request.userId(),
                request.totalAmount(),
                System.currentTimeMillis()
        );

        // O envio é assíncrono. Usamos um CompletableFuture para lidar com o callback.
        // É CRUCIAL usar a orderId como CHAVE (segundo argumento).[47]
        // Isso garante que todos os eventos para este pedido caiam na mesma partição (Ver Seção 10).
        CompletableFuture<SendResult<String, OrderCreatedEvent>> future =
                kafkaTemplate.send(TOPIC, event.getOrderId(), event);

        // Adiciona um callback para sucesso ou falha [46]
        future.whenComplete((result, ex) -> {
            if (ex == null) {
                System.out.println("Evento enviado com sucesso: " + event.getOrderId() +
                        " | Partição: " + result.getRecordMetadata().partition() +
                        " | Offset: " + result.getRecordMetadata().offset());
            } else {
                // Log de falha. Em produção, isso deve acionar um retry,
                System.err.println("Falha ao enviar evento: " + event.getOrderId() +
                        " | Erro: " + ex.getMessage());
            }
        });
    }
}
