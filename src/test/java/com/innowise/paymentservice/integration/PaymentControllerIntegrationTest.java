package com.innowise.paymentservice.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;

import com.innowise.paymentservice.dto.request.CreatePaymentRequest;
import com.innowise.paymentservice.dto.response.PaymentResponse;
import com.innowise.paymentservice.model.OutboxEvent;
import com.innowise.paymentservice.model.Payment;
import com.innowise.paymentservice.model.enums.PaymentStatus;
import com.innowise.paymentservice.repository.OutboxEventRepository;
import com.innowise.paymentservice.repository.PaymentRepository;
import com.innowise.paymentservice.utils.PaymentTestDataFactory;

@DisplayName("Payment API integration tests (Controller → Service → Repository → DB)")
class PaymentControllerIntegrationTest extends AbstractIntegrationTest {

    private static final UUID USER_A = UUID.fromString("aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaa1");
    private static final UUID USER_B = UUID.fromString("bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbb1");
    private static final UUID USER_C = UUID.fromString("cccccccc-cccc-4ccc-8ccc-ccccccccccc1");

    private static final String USER_A_EMAIL = "buyer@example.com";
    private static final String USER_B_EMAIL = "other@example.com";
    private static final String USER_C_EMAIL = "admin@example.com";

    private static final String PAYMENT_A1_ID = "payment-a-1";
    private static final String PAYMENT_A2_ID = "payment-a-2";
    private static final String PAYMENT_B1_ID = "payment-b-1";

    private static final String ORDER_A1_ID = "order-a-1";
    private static final String ORDER_A2_ID = "order-a-2";
    private static final String ORDER_B1_ID = "order-b-1";

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @BeforeEach
    void resetState() {
        paymentRepository.deleteAll();
        outboxEventRepository.deleteAll();

        Set<String> keys = redisTemplate.keys("idempotency:*");
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }

    private Payment seedPayment(
        String paymentId,
        String orderId,
        UUID userId,
        PaymentStatus status,
        BigDecimal amount,
        Instant createdAt
    ) {
        return paymentRepository.save(
            PaymentTestDataFactory.buildPayment(paymentId, orderId, userId.toString(), status, amount, createdAt)
        );
    }

    private Payment seedPayment(
        String paymentId,
        String orderId,
        UUID userId,
        PaymentStatus status,
        BigDecimal amount
    ) {
        return seedPayment(paymentId, orderId, userId, status, amount, Instant.parse("2025-06-01T12:00:00Z"));
    }

    @Nested
    @DisplayName("POST /payments")
    class CreatePayment {

        @Test
        @DisplayName("creates payment and outbox event")
        void whenAcquiringSucceeds_createsPaymentAndOutboxEvent() {
            String idempotencyKey = UUID.randomUUID().toString();
            stubRandomOrgResponse(2);

            CreatePaymentRequest request = PaymentTestDataFactory.buildCreatePaymentRequest();

            PaymentResponse response = webTestClient
                .post()
                .uri("/payments")
                .header("X-User-Id", USER_A.toString())
                .header("X-User-Email", USER_A_EMAIL)
                .header("X-User-Roles", "ROLE_USER")
                .header("Idempotency-Key", idempotencyKey)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isCreated()
                .expectBody(PaymentResponse.class)
                .returnResult()
                .getResponseBody();

            assertThat(response).isNotNull();
            assertThat(response.id()).isNotNull();
            assertThat(response.orderId()).isEqualTo(request.orderId());
            assertThat(response.userId()).isEqualTo(USER_A.toString());
            assertThat(response.status()).isEqualTo(PaymentStatus.SUCCESS);
            assertThat(response.paymentAmount()).isEqualByComparingTo(request.paymentAmount());

            Payment storedPayment = paymentRepository.findById(response.id()).orElseThrow();
            assertThat(storedPayment.getStatus()).isEqualTo(PaymentStatus.SUCCESS);
            assertThat(storedPayment.getOrderId()).isEqualTo(request.orderId());
            assertThat(storedPayment.getUserId()).isEqualTo(USER_A.toString());

            OutboxEvent outbox = outboxEventRepository.findAll().getFirst();
            assertThat(outbox.getAggregateId()).isEqualTo(request.orderId());
            assertThat(outbox.getEventType()).isEqualTo("CREATE_PAYMENT");
            assertThat(outbox.isProcessed()).isFalse();
            assertThat(outbox.getPayload()).contains("\"status\":\"SUCCESS\"");

            verifyRandomOrgCalledTimes(1);
        }

        @Test
        @DisplayName("reuses cached response for duplicate idempotency key")
        void whenSameIdempotencyKeyRepeats_returnsCachedResponse() {
            String idempotencyKey = UUID.randomUUID().toString();
            stubRandomOrgResponse(2);

            CreatePaymentRequest request = PaymentTestDataFactory.buildCreatePaymentRequest();

            PaymentResponse first = webTestClient
                .post()
                .uri("/payments")
                .header("X-User-Id", USER_A.toString())
                .header("X-User-Email", USER_A_EMAIL)
                .header("X-User-Roles", "ROLE_USER")
                .header("Idempotency-Key", idempotencyKey)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isCreated()
                .expectBody(PaymentResponse.class)
                .returnResult()
                .getResponseBody();

            PaymentResponse second = webTestClient
                .post()
                .uri("/payments")
                .header("X-User-Id", USER_A.toString())
                .header("X-User-Email", USER_A_EMAIL)
                .header("X-User-Roles", "ROLE_USER")
                .header("Idempotency-Key", idempotencyKey)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isCreated()
                .expectBody(PaymentResponse.class)
                .returnResult()
                .getResponseBody();

            assertThat(first).isNotNull();
            assertThat(second).isNotNull();
            assertThat(second.id()).isEqualTo(first.id());
            assertThat(paymentRepository.count()).isEqualTo(1);
            assertThat(outboxEventRepository.count()).isEqualTo(1);
            verifyRandomOrgCalledTimes(1);
        }

        @Test
        @DisplayName("returns 400 when Idempotency-Key is missing")
        void whenIdempotencyKeyMissing_returns400() {
            stubRandomOrgResponse(2);

            webTestClient
                .post()
                .uri("/payments")
                .header("X-User-Id", USER_A.toString())
                .header("X-User-Email", USER_A_EMAIL)
                .header("X-User-Roles", "ROLE_USER")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(PaymentTestDataFactory.buildCreatePaymentRequest())
                .exchange()
                .expectStatus().isBadRequest();
        }
    }

    @Nested
    @DisplayName("GET /payments/{id}")
    class GetPaymentById {

        @Test
        @DisplayName("returns payment for owner")
        void whenOwnerRequestsPayment_returns200() {
            Payment stored = seedPayment(
                PAYMENT_A1_ID,
                ORDER_A1_ID,
                USER_A,
                PaymentStatus.SUCCESS,
                new BigDecimal("12.50")
            );

            webTestClient
                .get()
                .uri("/payments/{id}", stored.getId())
                .header("X-User-Id", USER_A.toString())
                .header("X-User-Email", USER_A_EMAIL)
                .header("X-User-Roles", "ROLE_USER")
                .exchange()
                .expectStatus().isOk()
                .expectBody(PaymentResponse.class)
                .value(response -> {
                    assertThat(response).isNotNull();
                    assertThat(response.id()).isEqualTo(stored.getId());
                    assertThat(response.orderId()).isEqualTo(ORDER_A1_ID);
                    assertThat(response.userId()).isEqualTo(USER_A.toString());
                    assertThat(response.status()).isEqualTo(PaymentStatus.SUCCESS);
                    assertThat(response.paymentAmount()).isEqualByComparingTo("12.50");
                });
        }

        @Test
        @DisplayName("returns 403 for another user")
        void whenOtherUserRequestsPayment_returns403() {
            Payment stored = seedPayment(
                PAYMENT_A1_ID,
                ORDER_A1_ID,
                USER_A,
                PaymentStatus.SUCCESS,
                new BigDecimal("12.50")
            );

            webTestClient
                .get()
                .uri("/payments/{id}", stored.getId())
                .header("X-User-Id", USER_B.toString())
                .header("X-User-Email", USER_B_EMAIL)
                .header("X-User-Roles", "ROLE_USER")
                .exchange()
                .expectStatus().isForbidden();
        }

        @Test
        @DisplayName("returns 200 for admin on another user's payment")
        void whenAdminRequestsPayment_returns200() {
            Payment stored = seedPayment(
                PAYMENT_A1_ID,
                ORDER_A1_ID,
                USER_A,
                PaymentStatus.SUCCESS,
                new BigDecimal("12.50")
            );

            webTestClient
                .get()
                .uri("/payments/{id}", stored.getId())
                .header("X-User-Id", USER_C.toString())
                .header("X-User-Email", USER_C_EMAIL)
                .header("X-User-Roles", "ROLE_ADMIN")
                .exchange()
                .expectStatus().isOk()
                .expectBody(PaymentResponse.class)
                .value(response -> {
                    assertThat(response).isNotNull();
                    assertThat(response.id()).isEqualTo(stored.getId());
                });
        }

        @Test
        @DisplayName("returns 404 when payment does not exist")
        void whenPaymentMissing_returns404() {
            webTestClient
                .get()
                .uri("/payments/{id}", UUID.randomUUID().toString())
                .header("X-User-Id", USER_A.toString())
                .header("X-User-Email", USER_A_EMAIL)
                .header("X-User-Roles", "ROLE_USER")
                .exchange()
                .expectStatus().isNotFound();
        }
    }

    @Nested
    @DisplayName("GET /payments")
    class GetPayments {

        @Test
        @DisplayName("returns only current user's payments for non-admin")
        void whenUserRequestsPayments_returnsOwnOnly() {
            seedPayment(PAYMENT_A1_ID, ORDER_A1_ID, USER_A, PaymentStatus.SUCCESS, new BigDecimal("10.00"));
            seedPayment(PAYMENT_A2_ID, ORDER_A2_ID, USER_A, PaymentStatus.FAILED, new BigDecimal("20.00"));
            seedPayment(PAYMENT_B1_ID, ORDER_B1_ID, USER_B, PaymentStatus.SUCCESS, new BigDecimal("30.00"));

            webTestClient
                .get()
                .uri(uriBuilder -> uriBuilder
                    .path("/payments")
                    .queryParam("page", 0)
                    .queryParam("size", 20)
                    .queryParam("userId", USER_B.toString())
                    .build())
                .header("X-User-Id", USER_A.toString())
                .header("X-User-Email", USER_A_EMAIL)
                .header("X-User-Roles", "ROLE_USER")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.totalElements").isEqualTo(2)
                .jsonPath("$.content[0].userId").isEqualTo(USER_A.toString())
                .jsonPath("$.content[1].userId").isEqualTo(USER_A.toString());
        }

        @Test
        @DisplayName("allows admin to query by explicit userId")
        void whenAdminRequestsPayments_returnsFilteredPage() {
            seedPayment(PAYMENT_A1_ID, ORDER_A1_ID, USER_A, PaymentStatus.SUCCESS, new BigDecimal("10.00"));
            seedPayment(PAYMENT_B1_ID, ORDER_B1_ID, USER_B, PaymentStatus.SUCCESS, new BigDecimal("30.00"));

            webTestClient
                .get()
                .uri(uriBuilder -> uriBuilder
                    .path("/payments")
                    .queryParam("page", 0)
                    .queryParam("size", 20)
                    .queryParam("userId", USER_B.toString())
                    .build())
                .header("X-User-Id", USER_C.toString())
                .header("X-User-Email", USER_C_EMAIL)
                .header("X-User-Roles", "ROLE_ADMIN")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.totalElements").isEqualTo(1)
                .jsonPath("$.content[0].userId").isEqualTo(USER_B.toString())
                .jsonPath("$.content[0].orderId").isEqualTo(ORDER_B1_ID);
        }
    }

    @Nested
    @DisplayName("GET /payments/advanced")
    class GetAdvancedPayments {

        @Test
        @DisplayName("returns only current user's matching payments for non-admin")
        void whenUserRequestsAdvancedPayments_returnsOwnOnly() {
            Payment paymentA1 = seedPayment(
                PAYMENT_A1_ID,
                ORDER_A1_ID,
                USER_A,
                PaymentStatus.SUCCESS,
                new BigDecimal("10.00"),
                Instant.parse("2025-06-01T12:00:00Z")
            );
            Payment paymentA2 = seedPayment(
                PAYMENT_A2_ID,
                ORDER_A2_ID,
                USER_A,
                PaymentStatus.FAILED,
                new BigDecimal("20.00"),
                Instant.parse("2025-06-01T13:00:00Z")
            );
            seedPayment(
                PAYMENT_B1_ID,
                ORDER_B1_ID,
                USER_B,
                PaymentStatus.SUCCESS,
                new BigDecimal("30.00"),
                Instant.parse("2025-06-01T14:00:00Z")
            );
            Instant from = paymentA1.getCreatedAt().minusSeconds(1);
            Instant to = paymentA2.getCreatedAt().plusSeconds(1);

            webTestClient
                .get()
                .uri(uriBuilder -> uriBuilder
                    .path("/payments/advanced")
                    .queryParam("page", 0)
                    .queryParam("size", 20)
                    .queryParam("userId", USER_B.toString())
                    .queryParam("statuses", "SUCCESS")
                    .queryParam("createdFrom", from.toString())
                    .queryParam("createdTo", to.toString())
                    .build())
                .header("X-User-Id", USER_A.toString())
                .header("X-User-Email", USER_A_EMAIL)
                .header("X-User-Roles", "ROLE_USER")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.totalElements").isEqualTo(1)
                .jsonPath("$.content[0].userId").isEqualTo(USER_A.toString())
                .jsonPath("$.content[0].status").isEqualTo("SUCCESS");
        }

        @Test
        @DisplayName("allows admin to query by statuses and date range")
        void whenAdminRequestsAdvancedPayments_returnsFilteredPage() {
            Payment paymentA1 = seedPayment(
                PAYMENT_A1_ID,
                ORDER_A1_ID,
                USER_A,
                PaymentStatus.SUCCESS,
                new BigDecimal("10.00"),
                Instant.parse("2025-06-01T12:00:00Z")
            );
            Payment paymentB1 = seedPayment(
                PAYMENT_B1_ID,
                ORDER_B1_ID,
                USER_B,
                PaymentStatus.FAILED,
                new BigDecimal("30.00"),
                Instant.parse("2025-06-01T14:00:00Z")
            );
            Instant from = paymentA1.getCreatedAt().minusSeconds(1);
            Instant to = paymentB1.getCreatedAt().plusSeconds(1);

            webTestClient
                .get()
                .uri(uriBuilder -> uriBuilder
                    .path("/payments/advanced")
                    .queryParam("page", 0)
                    .queryParam("size", 20)
                    .queryParam("userId", USER_B.toString())
                    .queryParam("statuses", "FAILED")
                    .queryParam("createdFrom", from.toString())
                    .queryParam("createdTo", to.toString())
                    .build())
                .header("X-User-Id", USER_C.toString())
                .header("X-User-Email", USER_C_EMAIL)
                .header("X-User-Roles", "ROLE_ADMIN")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.totalElements").isEqualTo(1)
                .jsonPath("$.content[0].userId").isEqualTo(USER_B.toString())
                .jsonPath("$.content[0].status").isEqualTo("FAILED")
                .jsonPath("$.content[0].orderId").isEqualTo(ORDER_B1_ID);
        }
    }

    @Nested
    @DisplayName("GET /payments/summary/me")
    class GetMySummary {

        @Test
        @DisplayName("returns total amount for current user")
        void whenUserRequestsSummary_returnsOwnTotal() {
            Payment paymentA1 = seedPayment(PAYMENT_A1_ID, ORDER_A1_ID, USER_A, PaymentStatus.SUCCESS, new BigDecimal("10.25"));
            Payment paymentA2 = seedPayment(PAYMENT_A2_ID, ORDER_A2_ID, USER_A, PaymentStatus.FAILED, new BigDecimal("20.75"));
            seedPayment(PAYMENT_B1_ID, ORDER_B1_ID, USER_B, PaymentStatus.SUCCESS, new BigDecimal("100.00"));
            Instant from = paymentA1.getCreatedAt().minusSeconds(1);
            Instant to = paymentA2.getCreatedAt().plusSeconds(1);

            webTestClient
                .get()
                .uri(uriBuilder -> uriBuilder
                    .path("/payments/summary/me")
                    .queryParam("from", from.toString())
                    .queryParam("to", to.toString())
                    .build())
                .header("X-User-Id", USER_A.toString())
                .header("X-User-Email", USER_A_EMAIL)
                .header("X-User-Roles", "ROLE_USER")
                .exchange()
                .expectStatus().isOk()
                .expectBody(BigDecimal.class)
                .value(total -> assertThat(total).isEqualByComparingTo("10.25"));
        }

        @Test
        @DisplayName("returns zero when range contains only other users' payments")
        void whenUserHasNoOwnPaymentsInRange_returnsZero() {
            seedPayment(PAYMENT_B1_ID, ORDER_B1_ID, USER_B, PaymentStatus.SUCCESS, new BigDecimal("99.00"));
            Instant from = Instant.parse("2025-06-01T00:00:00Z");
            Instant to = Instant.parse("2025-06-02T00:00:00Z");

            webTestClient
                .get()
                .uri(uriBuilder -> uriBuilder
                    .path("/payments/summary/me")
                    .queryParam("from", from.toString())
                    .queryParam("to", to.toString())
                    .build())
                .header("X-User-Id", USER_A.toString())
                .header("X-User-Email", USER_A_EMAIL)
                .header("X-User-Roles", "ROLE_USER")
                .exchange()
                .expectStatus().isOk()
                .expectBody(BigDecimal.class)
                .value(total -> assertThat(total).isEqualByComparingTo("0"));
        }

        @Test
        @DisplayName("returns zero when own payments fall outside the requested window")
        void whenOwnPaymentsOutsideRange_returnsZero() {
            seedPayment(
                PAYMENT_A1_ID,
                ORDER_A1_ID,
                USER_A,
                PaymentStatus.SUCCESS,
                new BigDecimal("15.00"),
                Instant.parse("2025-06-01T12:00:00Z")
            );
            Instant from = Instant.parse("2025-07-01T00:00:00Z");
            Instant to = Instant.parse("2025-07-02T00:00:00Z");

            webTestClient
                .get()
                .uri(uriBuilder -> uriBuilder
                    .path("/payments/summary/me")
                    .queryParam("from", from.toString())
                    .queryParam("to", to.toString())
                    .build())
                .header("X-User-Id", USER_A.toString())
                .header("X-User-Email", USER_A_EMAIL)
                .header("X-User-Roles", "ROLE_USER")
                .exchange()
                .expectStatus().isOk()
                .expectBody(BigDecimal.class)
                .value(total -> assertThat(total).isEqualByComparingTo("0"));
        }

        @Test
        @DisplayName("admin calling /summary/me still sums only that principal's payments")
        void whenAdminRequestsOwnSummary_returnsTotalForPrincipalOnly() {
            seedPayment(PAYMENT_A1_ID, ORDER_A1_ID, USER_A, PaymentStatus.SUCCESS, new BigDecimal("40.00"));
            Payment adminPayment = seedPayment(
                "payment-admin-1",
                "order-admin-1",
                USER_C,
                PaymentStatus.SUCCESS,
                new BigDecimal("7.50"),
                Instant.parse("2025-06-01T12:00:00Z")
            );
            Instant from = adminPayment.getCreatedAt().minusSeconds(1);
            Instant to = adminPayment.getCreatedAt().plusSeconds(1);

            webTestClient
                .get()
                .uri(uriBuilder -> uriBuilder
                    .path("/payments/summary/me")
                    .queryParam("from", from.toString())
                    .queryParam("to", to.toString())
                    .build())
                .header("X-User-Id", USER_C.toString())
                .header("X-User-Email", USER_C_EMAIL)
                .header("X-User-Roles", "ROLE_ADMIN")
                .exchange()
                .expectStatus().isOk()
                .expectBody(BigDecimal.class)
                .value(total -> assertThat(total).isEqualByComparingTo("7.50"));
        }
    }

    @Nested
    @DisplayName("GET /payments/summary")
    class GetAllSummary {

        @Test
        @DisplayName("returns total amount for admin")
        void whenAdminRequestsSummary_returnsTotal() {
            Payment paymentA1 = seedPayment(PAYMENT_A1_ID, ORDER_A1_ID, USER_A, PaymentStatus.SUCCESS, new BigDecimal("10.25"));
            seedPayment(PAYMENT_A2_ID, ORDER_A2_ID, USER_A, PaymentStatus.FAILED, new BigDecimal("20.75"));
            Payment paymentB1 = seedPayment(PAYMENT_B1_ID, ORDER_B1_ID, USER_B, PaymentStatus.SUCCESS, new BigDecimal("100.00"));
            Instant from = paymentA1.getCreatedAt().minusSeconds(1);
            Instant to = paymentB1.getCreatedAt().plusSeconds(1);

            webTestClient
                .get()
                .uri(uriBuilder -> uriBuilder
                    .path("/payments/summary")
                    .queryParam("from", from.toString())
                    .queryParam("to", to.toString())
                    .build())
                .header("X-User-Id", USER_C.toString())
                .header("X-User-Email", USER_C_EMAIL)
                .header("X-User-Roles", "ROLE_ADMIN")
                .exchange()
                .expectStatus().isOk()
                .expectBody(BigDecimal.class)
                .value(total -> assertThat(total).isEqualByComparingTo("110.25"));
        }

        @Test
        @DisplayName("returns 403 for non-admin")
        void whenUserRequestsSummary_returns403() {
            webTestClient
                .get()
                .uri(uriBuilder -> uriBuilder
                    .path("/payments/summary")
                    .queryParam("from", "2025-06-01T00:00:00Z")
                    .queryParam("to", "2025-06-02T00:00:00Z")
                    .build())
                .header("X-User-Id", USER_A.toString())
                .header("X-User-Email", USER_A_EMAIL)
                .header("X-User-Roles", "ROLE_USER")
                .exchange()
                .expectStatus().isForbidden();
        }
    }
}
