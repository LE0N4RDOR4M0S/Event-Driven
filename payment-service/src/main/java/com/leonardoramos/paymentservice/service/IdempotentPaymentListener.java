package com.leonardoramos.paymentservice.service;

import com.leonardoramos.paymentservice.events.OrderCreatedEvent;
import com.leonardoramos.paymentservice.model.Payment;
import com.leonardoramos.paymentservice.model.ProcessedEvent;
import com.leonardoramos.paymentservice.repository.PaymentRepository;
import com.leonardoramos.paymentservice.repository.ProcessedEventRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.DefaultTransactionDefinition;

@Service
public class IdempotentPaymentListener {
    private final PaymentRepository paymentRepository;
    private final ProcessedEventRepository eventRepository;
    private final PlatformTransactionManager transactionManager;

    private IdempotentPaymentListener(
            PaymentRepository paymentRepository,
            ProcessedEventRepository eventRepository,
            PlatformTransactionManager transactionManager) {
        this.paymentRepository = paymentRepository;
        this.eventRepository = eventRepository;
        this.transactionManager = transactionManager;
    }

    @KafkaListener(
            topics = "orders",
            groupId = "payment-group-idempotent"
    )
    @Transactional
    public void handleOrderEvent(OrderCreatedEvent event) {

        if (eventRepository.existsById(event.getEventId())) {
            System.out.println("Evento duplicado recebido (ignorado): " + event.getEventId());
            return;
        }

        TransactionStatus status = transactionManager.getTransaction(new DefaultTransactionDefinition());

        try {
            Payment payment = new Payment(null,event.getOrderId(), event.getTotalAmount());
            paymentRepository.save(payment);

            ProcessedEvent processed = new ProcessedEvent(event.getEventId());
            eventRepository.save(processed);

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
