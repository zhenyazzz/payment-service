package com.innowise.paymentservice.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.UUID;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;

import com.innowise.paymentservice.model.OutboxEvent;
import com.innowise.paymentservice.producer.OutboxScheduler;
import com.innowise.paymentservice.repository.OutboxEventRepository;

@DisplayName("Outbox scheduler integration (Mongo + Kafka Testcontainers)")
class OutboxSchedulerIntegrationTest extends AbstractIntegrationTest {

    private static final String CREATE_PAYMENT = "CREATE_PAYMENT";

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    @Autowired
    private OutboxScheduler outboxScheduler;

    @Autowired
    private Environment environment;

    private String topicName() {
        return environment.getRequiredProperty("kafka.topic.name");
    }

    private String bootstrapServers() {
        return environment.getRequiredProperty("spring.kafka.bootstrap-servers");
    }

    private static String createPaymentPayload(String orderId, String status) {
        return ("{\"paymentId\":\"pay-test\",\"orderId\":\"%s\",\"userId\":\"user-test\",\"status\":\"%s\"}")
            .formatted(orderId, status);
    }

    @BeforeEach
    void resetState() {
        outboxEventRepository.deleteAll();
    }

    private OutboxEvent buildEvent(String orderId, String status) {
        return OutboxEvent.builder()
            .aggregateId(orderId)
            .eventType(CREATE_PAYMENT)
            .payload(createPaymentPayload(orderId, status))
            .processed(false)
            .build();
    }

    private static KafkaConsumer<String, String> newConsumer(String bootstrap, String topic, String groupId) {
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrap);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");
        KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props);
        consumer.subscribe(List.of(topic));
        return consumer;
    }

    @Test
    @DisplayName("writes outbox to Mongo, sends to real Kafka topic, marks processed")
    void relaysToKafkaAndMarksProcessed() {
        String orderId = "order-outbox-" + UUID.randomUUID();
        String topic = topicName();
        String payload = createPaymentPayload(orderId, "SUCCESS");
        String groupId = "outbox-it-" + UUID.randomUUID();

        try (KafkaConsumer<String, String> consumer = newConsumer(bootstrapServers(), topic, groupId)) {
            await().atMost(Duration.ofSeconds(15)).untilAsserted(() -> {
                consumer.poll(Duration.ofMillis(500));
                assertThat(consumer.assignment()).isNotEmpty();
            });

            OutboxEvent event = buildEvent(orderId, "SUCCESS");
            outboxEventRepository.save(event);

            outboxScheduler.processOutboxEvents();

            List<ConsumerRecord<String, String>> received = new ArrayList<>();
            await().atMost(Duration.ofSeconds(25)).untilAsserted(() -> {
                consumer.poll(Duration.ofMillis(800)).forEach(received::add);
                assertThat(received)
                    .anyMatch(r ->
                        topic.equals(r.topic()) && orderId.equals(r.key()) && payload.equals(r.value()));
            });

            assertThat(outboxEventRepository.findById(event.getId()).orElseThrow().isProcessed()).isTrue();
        }
    }
}
