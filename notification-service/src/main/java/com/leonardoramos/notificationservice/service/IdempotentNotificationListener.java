package com.leonardoramos.notificationservice.service;

import com.leonardoramos.notificationservice.events.OrderCreatedEvent;
import com.leonardoramos.notificationservice.model.Notification;
import com.leonardoramos.notificationservice.model.ProcessedEvent;
import com.leonardoramos.notificationservice.repository.NotificationRepository;
import com.leonardoramos.notificationservice.repository.ProcessedEventRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.DefaultTransactionDefinition;

import java.util.UUID;

@Service
public class IdempotentNotificationListener {
    private final NotificationRepository notificationRepository;
    private final ProcessedEventRepository processedEventRepository;
    private final PlatformTransactionManager transactionManager;

    public IdempotentNotificationListener(NotificationRepository notificationRepository,
                                          ProcessedEventRepository processedEventRepository,
                                          PlatformTransactionManager transactionManager) {
        this.notificationRepository = notificationRepository;
        this.processedEventRepository = processedEventRepository;
        this.transactionManager = transactionManager;
    }

    @KafkaListener(
            topics = "orders",
            groupId = "notification-group-idempotent"
    )
    @Transactional
    public void handleOrderEvent(OrderCreatedEvent event) {

        if (processedEventRepository.existsById(event.getEventId())) {
            System.out.println("Evento duplicado recebido (ignorado): " + event.getEventId());
            return;
        }

        TransactionStatus status = transactionManager.getTransaction(new DefaultTransactionDefinition());

        try {
            Notification payment = new Notification(null,event.getOrderId());
            notificationRepository.save(payment);

            ProcessedEvent processed = new ProcessedEvent(event.getEventId());
            processedEventRepository.save(processed);

            transactionManager.commit(status);
            System.out.println("Pagamento processado (IDEMPOTENTE): " + event.getOrderId());

        } catch (DataIntegrityViolationException e) {
            // Falha de Chave Primária
            transactionManager.rollback(status);
            System.out.println("Duplicata (concorrente) detectada, rollback: " + event.getEventId());
            // Não lançamos a exceção, pois a duplicata foi tratada.

        } catch (Exception e) {
            transactionManager.rollback(status);
            System.err.println("Falha ao processar pagamento: " + e.getMessage());

            // Importante: Lançar a exceção para que o Spring/Kafka NÃO comite o offset, forçando a re-entrega.
            throw new RuntimeException("Falha no processamento, será tentado novamente.", e);
        }
    }
}
