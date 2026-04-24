package com.innowise.paymentservice.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.innowise.paymentservice.model.OutboxEvent;
import com.innowise.paymentservice.producer.OutboxScheduler;
import com.innowise.paymentservice.repository.OutboxEventRepository;

@DisplayName("Outbox scheduler integration tests")
class OutboxSchedulerIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    @Autowired
    private OutboxScheduler outboxScheduler;

    @MockitoBean
    private KafkaTemplate<String, String> kafkaTemplate;

    @BeforeEach
    void resetState() {
        outboxEventRepository.deleteAll();
    }

    private OutboxEvent buildEvent(String aggregateId) {
        return OutboxEvent.builder()
            .aggregateId(aggregateId)
            .eventType("CREATE_PAYMENT")
            .payload("{\"status\":\"SUCCESS\"}")
            .processed(false)
            .build();
    }

    @Nested
    @DisplayName("when Kafka send succeeds")
    class SuccessfulProcessing {

        @Test
        @DisplayName("marks the outbox event as processed")
        void shouldProcessOutboxEventSuccessfully() {
            OutboxEvent event = buildEvent("order-777");
            outboxEventRepository.save(event);

            CompletableFuture<org.springframework.kafka.support.SendResult<String, String>> completedSend =
                CompletableFuture.completedFuture(null);

            when(kafkaTemplate.send(eq("test-payment-events"), anyString(), eq("{\"status\":\"SUCCESS\"}")))
                .thenReturn(completedSend);

            outboxScheduler.processOutboxEvents();

            OutboxEvent updatedEvent = outboxEventRepository.findById(event.getId()).orElseThrow();
            assertThat(updatedEvent.isProcessed()).isTrue();
        }
    }

    @Nested
    @DisplayName("when Kafka send fails")
    class FailedProcessing {

        @Test
        @DisplayName("leaves the outbox event unprocessed")
        void keepsEventUnprocessedWhenKafkaSendFails() {
            OutboxEvent event = buildEvent("order-888");
            outboxEventRepository.save(event);

            CompletableFuture<org.springframework.kafka.support.SendResult<String, String>> failedSend =
                new CompletableFuture<>();
            failedSend.completeExceptionally(new RuntimeException("Kafka is down"));

            when(kafkaTemplate.send(eq("test-payment-events"), anyString(), eq("{\"status\":\"SUCCESS\"}")))
                .thenReturn(failedSend);

            outboxScheduler.processOutboxEvents();

            OutboxEvent reloaded = outboxEventRepository.findById(event.getId()).orElseThrow();
            assertThat(reloaded.isProcessed()).isFalse();
        }
    }
}
