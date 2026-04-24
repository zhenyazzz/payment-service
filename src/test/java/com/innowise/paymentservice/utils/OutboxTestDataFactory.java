package com.innowise.paymentservice.utils;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.springframework.kafka.support.SendResult;

import com.innowise.paymentservice.model.OutboxEvent;

import lombok.experimental.UtilityClass;

@UtilityClass
public class OutboxTestDataFactory {

    public static final String KAFKA_UNIT_TEST_TOPIC = "payments-topic";

    public static final String EVENT_ID_1 = "evt-1";
    public static final String EVENT_ID_2 = "evt-2";
    public static final String ORDER_ID_1 = "order-1";
    public static final String ORDER_ID_2 = "order-2";

    public static String payloadWithStatus(String status) {
        return "{\"status\":\"" + status + "\"}";
    }

    public static String emptyJsonPayload() {
        return "{}";
    }

    public static OutboxEvent outboxEvent(String eventId, String orderId, String payload) {
        return OutboxEvent.builder()
            .id(eventId)
            .aggregateId(orderId)
            .payload(payload)
            .processed(false)
            .build();
    }

    public static List<OutboxEvent> twoPendingOutboxEventsForSuccessfulSend() {
        return List.of(
            outboxEvent(EVENT_ID_1, ORDER_ID_1, payloadWithStatus("SUCCESS")),
            outboxEvent(EVENT_ID_2, ORDER_ID_2, payloadWithStatus("FAILED"))
        );
    }

    public static OutboxEvent singlePendingOutboxEventForFailedSend() {
        return outboxEvent(EVENT_ID_1, ORDER_ID_1, emptyJsonPayload());
    }

    public static CompletableFuture<SendResult<String, String>> completedKafkaSend() {
        return CompletableFuture.completedFuture(null);
    }

    public static CompletableFuture<SendResult<String, String>> failedKafkaSend(Throwable cause) {
        CompletableFuture<SendResult<String, String>> future = new CompletableFuture<>();
        future.completeExceptionally(cause);
        return future;
    }
}
