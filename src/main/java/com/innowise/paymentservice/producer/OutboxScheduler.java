package com.innowise.paymentservice.producer;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.nio.charset.StandardCharsets;

import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.innowise.paymentservice.model.OutboxEvent;
import com.innowise.paymentservice.repository.OutboxEventRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxScheduler {

    private static final int OUTBOX_FETCH_BATCH_SIZE = 100;
    private static final String EVENT_TYPE_HEADER = "event_type";
    private static final String EVENT_ID_HEADER = "event_id";

    private final OutboxEventRepository outboxEventRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    @Value("${kafka.topic.name}")
    private String topicName;

    @Scheduled(
        fixedDelayString = "${outbox.scheduler.fixed-delay-ms:5000}"
    )
    public void processOutboxEvents() {
        List<OutboxEvent> events = outboxEventRepository.findByProcessedFalseOrderByCreatedAtAsc(
            PageRequest.of(0, OUTBOX_FETCH_BATCH_SIZE)
        );

        if (events.isEmpty()) {
            return;
        }

        log.info("Found {} outbox events to process", events.size());

        List<CompletableFuture<String>> futures = events.stream()
            .map(event -> kafkaTemplate
                .send(toProducerRecord(event))
                .thenApply(result -> event.getId())
                .exceptionally(ex -> {
                    log.error("Failed to send event to Kafka", ex);
                    return null;
                }))
            .toList();

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        List<String> successfulEventIds = futures.stream()
            .map(CompletableFuture::join)
            .filter(Objects::nonNull)
            .toList();

        if (!successfulEventIds.isEmpty()) {
            outboxEventRepository.markAsProcessed(successfulEventIds);
            log.info("Marked {} events as processed", successfulEventIds.size());
        }
    }

    private ProducerRecord<String, String> toProducerRecord(OutboxEvent event) {
        ProducerRecord<String, String> record = new ProducerRecord<>(
            topicName,
            event.getAggregateId(),
            event.getPayload()
        );
        record.headers().add(EVENT_TYPE_HEADER, event.getEventType().getBytes(StandardCharsets.UTF_8));
        record.headers().add(EVENT_ID_HEADER, event.getId().getBytes(StandardCharsets.UTF_8));
        return record;
    }
}
