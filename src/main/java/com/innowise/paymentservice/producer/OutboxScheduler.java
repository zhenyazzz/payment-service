package com.innowise.paymentservice.producer;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

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

    private final OutboxEventRepository outboxEventRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    @Value("${kafka.topic.name}")
    private String topicName;

    
    @Scheduled(fixedDelayString = "${outbox.scheduler.fixed-delay-ms}")
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
                .send(topicName, event.getAggregateId(), event.getPayload())
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
}
