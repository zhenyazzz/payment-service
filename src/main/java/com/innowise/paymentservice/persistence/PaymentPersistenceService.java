package com.innowise.paymentservice.persistence;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.innowise.paymentservice.mapper.PaymentMapper;
import com.innowise.paymentservice.model.OutboxEvent;
import com.innowise.paymentservice.model.Payment;
import com.innowise.paymentservice.model.enums.PaymentStatus;
import com.innowise.paymentservice.producer.PaymentCreatedEvent;
import com.innowise.paymentservice.repository.OutboxEventRepository;
import com.innowise.paymentservice.repository.PaymentRepository;

import lombok.RequiredArgsConstructor;
import tools.jackson.databind.ObjectMapper;

@Service
@RequiredArgsConstructor
public class PaymentPersistenceService {

    private final PaymentRepository paymentRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;
    private final PaymentMapper paymentMapper;


    public Payment savePendingPayment(Payment payment) {
        payment.setStatus(PaymentStatus.PENDING);
        return paymentRepository.save(payment);
    }

    @Transactional(transactionManager = "mongoTransactionManager")
    public Payment finalizePaymentAndCreateOutboxEvent(Payment payment, PaymentStatus finalStatus) {
        payment.setStatus(finalStatus);
        paymentRepository.save(payment);

        PaymentCreatedEvent paymentCreatedEvent = paymentMapper.toPaymentCreatedEvent(payment);

        OutboxEvent outboxEvent = OutboxEvent.builder()
            .aggregateId(payment.getOrderId())
            .eventType("CREATE_PAYMENT")
            .payload(objectMapper.writeValueAsString(paymentCreatedEvent))
            .build();
            
        outboxEventRepository.save(outboxEvent);
        return payment;
    }
}
