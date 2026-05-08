package com.innowise.paymentservice.unit;

import static com.innowise.paymentservice.utils.OutboxTestDataFactory.EVENT_ID_1;
import static com.innowise.paymentservice.utils.OutboxTestDataFactory.EVENT_ID_2;
import static com.innowise.paymentservice.utils.OutboxTestDataFactory.KAFKA_UNIT_TEST_TOPIC;
import static com.innowise.paymentservice.utils.OutboxTestDataFactory.ORDER_ID_1;
import static com.innowise.paymentservice.utils.OutboxTestDataFactory.ORDER_ID_2;
import static com.innowise.paymentservice.utils.OutboxTestDataFactory.completedKafkaSend;
import static com.innowise.paymentservice.utils.OutboxTestDataFactory.failedKafkaSend;
import static com.innowise.paymentservice.utils.OutboxTestDataFactory.payloadWithStatus;
import static com.innowise.paymentservice.utils.OutboxTestDataFactory.singlePendingOutboxEventForFailedSend;
import static com.innowise.paymentservice.utils.OutboxTestDataFactory.twoPendingOutboxEventsForSuccessfulSend;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.util.List;

import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import com.innowise.paymentservice.model.OutboxEvent;
import com.innowise.paymentservice.producer.OutboxScheduler;
import com.innowise.paymentservice.repository.OutboxEventRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("OutboxScheduler (unit)")
class OutboxSchedulerUnitTest {

    @Mock
    private OutboxEventRepository outboxEventRepository;

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    @InjectMocks
    private OutboxScheduler outboxScheduler;

    @SuppressWarnings("unchecked")
    private final ArgumentCaptor<ProducerRecord<String, String>> producerRecordCaptor =
        ArgumentCaptor.forClass((Class<ProducerRecord<String, String>>) (Class<?>) ProducerRecord.class);

    @BeforeEach
    void setTopic() {
        ReflectionTestUtils.setField(outboxScheduler, "topicName", KAFKA_UNIT_TEST_TOPIC);
    }

    @Nested
    class WhenThereAreNoEvents {

        @Test
        @DisplayName("does not send to Kafka and does not mark processed")
        void skipsWork() {
            when(outboxEventRepository.findByProcessedFalseOrderByCreatedAtAsc(any(PageRequest.class)))
                .thenReturn(List.of());

            outboxScheduler.processOutboxEvents();

            verifyNoInteractions(kafkaTemplate);
            verify(outboxEventRepository, never()).markAsProcessed(any());
        }
    }

    @Nested
    class WhenKafkaSendSucceeds {

        @Test
        @DisplayName("sends with topic, order key, payload then marks events processed")
        void sendsAndMarksProcessed() {
            List<OutboxEvent> events = twoPendingOutboxEventsForSuccessfulSend();
            when(outboxEventRepository.findByProcessedFalseOrderByCreatedAtAsc(any(PageRequest.class)))
                .thenReturn(events);

            when(kafkaTemplate.send(Mockito.<ProducerRecord<String, String>>any())).thenReturn(completedKafkaSend());

            outboxScheduler.processOutboxEvents();

            verify(kafkaTemplate, times(2)).send(producerRecordCaptor.capture());
            List<ProducerRecord<String, String>> sentRecords = producerRecordCaptor.getAllValues();

            ProducerRecord<String, String> firstRecord = sentRecords.get(0);
            ProducerRecord<String, String> secondRecord = sentRecords.get(1);

            assertSentRecord(firstRecord, ORDER_ID_1, payloadWithStatus("SUCCESS"), EVENT_ID_1);
            assertSentRecord(secondRecord, ORDER_ID_2, payloadWithStatus("FAILED"), EVENT_ID_2);

            verify(outboxEventRepository).markAsProcessed(argThat(ids ->
                ids.size() == 2 && ids.contains(EVENT_ID_1) && ids.contains(EVENT_ID_2)));
        }
    }

    @Nested
    class WhenKafkaSendFails {

        @Test
        @DisplayName("does not call markAsProcessed")
        void skipsMarkProcessed() {
            OutboxEvent event = singlePendingOutboxEventForFailedSend();
            when(outboxEventRepository.findByProcessedFalseOrderByCreatedAtAsc(any(PageRequest.class)))
                .thenReturn(List.of(event));

            when(kafkaTemplate.send(Mockito.<ProducerRecord<String, String>>any()))
                .thenReturn(failedKafkaSend(new RuntimeException("broker down")));

            outboxScheduler.processOutboxEvents();

            verify(outboxEventRepository, never()).markAsProcessed(any());
        }
    }

    private void assertSentRecord(
        ProducerRecord<String, String> record,
        String expectedOrderId,
        String expectedPayload,
        String expectedEventId
    ) {
        assertThat(record.topic()).isEqualTo(KAFKA_UNIT_TEST_TOPIC);
        assertThat(record.key()).isEqualTo(expectedOrderId);
        assertThat(record.value()).isEqualTo(expectedPayload);
        assertThat(new String(record.headers().lastHeader("event_type").value(), StandardCharsets.UTF_8))
            .isEqualTo("CREATE_PAYMENT");
        assertThat(new String(record.headers().lastHeader("event_id").value(), StandardCharsets.UTF_8))
            .isEqualTo(expectedEventId);
    }
}
