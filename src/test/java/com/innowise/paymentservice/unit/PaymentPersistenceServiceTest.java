package com.innowise.paymentservice.unit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.innowise.paymentservice.mapper.PaymentMapper;
import com.innowise.paymentservice.model.OutboxEvent;
import com.innowise.paymentservice.model.Payment;
import com.innowise.paymentservice.model.enums.PaymentStatus;
import com.innowise.paymentservice.persistence.PaymentPersistenceService;
import com.innowise.paymentservice.producer.PaymentCreatedEvent;
import com.innowise.paymentservice.repository.OutboxEventRepository;
import com.innowise.paymentservice.repository.PaymentRepository;
import com.innowise.paymentservice.utils.PaymentTestDataFactory;

import tools.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
@DisplayName("PaymentPersistenceService (unit tests)")
class PaymentPersistenceServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private OutboxEventRepository outboxEventRepository;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private PaymentMapper paymentMapper;

    @InjectMocks
    private PaymentPersistenceService paymentPersistenceService;

    @Captor
    private ArgumentCaptor<OutboxEvent> outboxCaptor;

    @Nested
    @DisplayName("savePendingPayment")
    class SavePendingPayment {

        @Test
        @DisplayName("sets status to PENDING and persists payment")
        void whenSavePendingPayment_setsPendingAndSaves() {
            Payment payment = PaymentTestDataFactory.buildPayment(PaymentStatus.SUCCESS);
            when(paymentRepository.save(payment)).thenReturn(payment);

            Payment result = paymentPersistenceService.savePendingPayment(payment);

            assertThat(result.getStatus()).isEqualTo(PaymentStatus.PENDING);
            verify(paymentRepository).save(payment);
        }

        @Test
        @DisplayName("propagates repository failures")
        void whenPaymentRepositoryThrows_propagatesException() {
            Payment payment = PaymentTestDataFactory.buildPayment();
            RuntimeException dbException = new RuntimeException("MongoDB is down");
            when(paymentRepository.save(any(Payment.class))).thenThrow(dbException);

            assertThatThrownBy(() -> paymentPersistenceService.savePendingPayment(payment))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("MongoDB is down");
        }
    }

    @Nested
    @DisplayName("finalizePaymentAndCreateOutboxEvent")
    class FinalizePaymentAndCreateOutboxEvent {

        @Test
        @DisplayName("creates outbox event on SUCCESS")
        void whenFinalizePayment_success_createsOutboxEvent() throws Exception {
            Payment payment = PaymentTestDataFactory.buildPayment();
            PaymentCreatedEvent createdEvent = PaymentTestDataFactory.buildPaymentCreatedEvent();
            String jsonPayload = "{\"status\":\"SUCCESS\"}";

            when(paymentRepository.save(payment)).thenReturn(payment);
            when(paymentMapper.toPaymentCreatedEvent(payment)).thenReturn(createdEvent);
            when(objectMapper.writeValueAsString(createdEvent)).thenReturn(jsonPayload);

            Payment result = paymentPersistenceService.finalizePaymentAndCreateOutboxEvent(
                payment,
                PaymentStatus.SUCCESS
            );

            assertThat(result.getStatus()).isEqualTo(PaymentStatus.SUCCESS);
            verify(paymentRepository).save(payment);
            verify(outboxEventRepository).save(outboxCaptor.capture());

            OutboxEvent outbox = outboxCaptor.getValue();
            assertThat(outbox.getAggregateId()).isEqualTo(PaymentTestDataFactory.ORDER_ID);
            assertThat(outbox.getEventType()).isEqualTo("CREATE_PAYMENT");
            assertThat(outbox.getPayload()).isEqualTo(jsonPayload);
        }

        @Test
        @DisplayName("creates outbox event on FAILED")
        void whenFinalizePayment_failed_createsOutboxEvent() throws Exception {
            Payment payment = PaymentTestDataFactory.buildPayment();
            PaymentCreatedEvent createdEvent = PaymentTestDataFactory.buildPaymentCreatedEvent(PaymentStatus.FAILED);
            String jsonPayload = "{\"status\":\"FAILED\"}";

            when(paymentRepository.save(payment)).thenReturn(payment);
            when(paymentMapper.toPaymentCreatedEvent(payment)).thenReturn(createdEvent);
            when(objectMapper.writeValueAsString(createdEvent)).thenReturn(jsonPayload);

            paymentPersistenceService.finalizePaymentAndCreateOutboxEvent(payment, PaymentStatus.FAILED);

            assertThat(payment.getStatus()).isEqualTo(PaymentStatus.FAILED);
            verify(outboxEventRepository).save(any(OutboxEvent.class));
        }

        @Test
        @DisplayName("does not create outbox event if JSON serialization fails")
        void whenObjectMapperThrows_doesNotCreateOutbox() throws Exception {
            Payment payment = PaymentTestDataFactory.buildPayment();
            PaymentCreatedEvent createdEvent = PaymentTestDataFactory.buildPaymentCreatedEvent();

            when(paymentRepository.save(payment)).thenReturn(payment);
            when(paymentMapper.toPaymentCreatedEvent(payment)).thenReturn(createdEvent);
            when(objectMapper.writeValueAsString(createdEvent)).thenThrow(new RuntimeException("json error"));

            assertThatThrownBy(() ->
                paymentPersistenceService.finalizePaymentAndCreateOutboxEvent(payment, PaymentStatus.SUCCESS)
            ).isInstanceOf(RuntimeException.class)
             .hasMessage("json error");

            verify(outboxEventRepository, never()).save(any());
        }

        @Test
        @DisplayName("propagates exception if outbox save fails")
        void whenOutboxSaveThrows_propagatesException() throws Exception {
            Payment payment = PaymentTestDataFactory.buildPayment();
            PaymentCreatedEvent createdEvent = PaymentTestDataFactory.buildPaymentCreatedEvent();

            when(paymentRepository.save(payment)).thenReturn(payment);
            when(paymentMapper.toPaymentCreatedEvent(payment)).thenReturn(createdEvent);
            when(objectMapper.writeValueAsString(createdEvent)).thenReturn("{}");

            RuntimeException dbException = new RuntimeException("Outbox DB is down");
            when(outboxEventRepository.save(any(OutboxEvent.class))).thenThrow(dbException);

            assertThatThrownBy(() ->
                paymentPersistenceService.finalizePaymentAndCreateOutboxEvent(payment, PaymentStatus.SUCCESS)
            ).isInstanceOf(RuntimeException.class)
             .hasMessage("Outbox DB is down");
        }
    }
}
