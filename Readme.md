# Payment Service

## At a Glance

- **Purpose:** process payment attempts for orders.
- **Source of truth:** MongoDB payment documents.
- **Sync dependencies:** `order-service`, external acquiring endpoint.
- **Async path:** outbox -> Kafka.
- **Idempotency:** `Idempotency-Key` for repeated HTTP requests.
- **Identity:** upstream gateway provides user context.

---

## Request Flow

```text
Client
  |
  v
Gateway / trusted identity headers
  |
  v
payment-service
  |--> order-service (price lookup)
  |--> acquiring endpoint (payment result)
  |--> MongoDB (payments + outbox)
  |--> Redis (idempotency state)
  |
  v
Outbox scheduler
  |
  v
Kafka
  |
  v
Downstream consumers
```

---

## What It Owns

- payment attempts;
- payment status transitions;
- payment amount resolved from order price;
- outbox events for payment state changes;
- idempotency state for create-payment requests.

### Payment Status Model

Current payment states in code:

- `PENDING` - payment attempt is created, but the final outcome is not persisted yet.
- `SUCCESS` - payment attempt completed successfully.
- `FAILED` - payment attempt finished with a failure outcome.

State flow:

```text
PENDING -> SUCCESS
PENDING -> FAILED
```

---

## What It Does Not Own

- order lifecycle;
- user profiles or authentication tokens;
- Kafka exactly-once delivery;
- downstream order fulfillment;
- global reporting outside payment data.

---

## Dependencies

### Synchronous

- `order-service` - resolves order total price before payment finalization.
- Acquiring endpoint - decides whether the payment attempt succeeds or fails.

### Asynchronous

- Kafka - receives payment outcome events.
- Redis - stores idempotency state.
- MongoDB - stores payments and outbox events.

---

## API Surface

### `POST /payments`

Creates a payment attempt for the current authenticated user.

Flow:
1. requires `Idempotency-Key`;
2. resolves the order price from `order-service`;
3. creates a pending payment record;
4. determines the final payment outcome;
5. persists the final state;
6. stores an outbox event.

### `GET /payments/{id}`

Returns a payment by id.

- owner can read;
- admin can read any payment.

### `GET /payments`

Returns paginated payments with filtering.

- non-admin users can read only their own payments;
- admins can query by explicit user filter.

### `GET /payments/advanced`

Returns paginated payments with advanced filters:

- user id;
- order id;
- payment statuses;
- created-at range.

### `GET /payments/summary/me`

Returns the total successful payment amount for the current user in a date range.

### `GET /payments/summary`

Returns the total successful payment amount across all users in a date range.

- admin only.

---

## Event Contract

- **Event type:** `CREATE_PAYMENT`
- **Event key:** order id
- **Payload fields:** payment id, order id, user id, amount, status, created-at
- **Delivery semantics:** at-least-once
- **Important:** consumers must tolerate duplicate delivery

---

## Failure Behavior

- `order-service` down -> payment creation fails.
- acquiring failure -> payment attempt is not finalized as successful.
- Kafka publish failure -> event stays in outbox and is retried.
- repeated identical request -> cached response from idempotency layer.
- invalid ownership or role -> `401`, `403`, or `404` depending on the point of failure.

---

## Idempotency Model

- `POST /payments` requires `Idempotency-Key`.
- key is scoped to method, URI, current user, and request payload hash.
- Redis stores processing state.
- same request returns cached response.
- same key with different payload is rejected.

This protects retries of the same HTTP request, not order-level business uniqueness.

---

## Persistence Model

### `payments`

Important fields:

- `id`
- `orderId`
- `userId`
- `status`
- `paymentAmount`
- `createdAt`
- `updatedAt`

### `outbox_events`

Important fields:

- `id`
- `aggregateId`
- `eventType`
- `payload`
- `processed`
- `createdAt`

---

## Observability

- **Logging:** service-layer logs include summarized args/results and trace id.
- **Tracing:** exported through OpenTelemetry Collector.
- **Metrics:** exported through OpenTelemetry Collector and Prometheus.
- **Health:** exposed via Spring Boot Actuator.

---

## Security Boundary

Upstream gateway provides the identity context.

Current headers:

- `X-User-Id`
- `X-User-Email`
- `X-User-Roles`

Authorization is enforced in Spring Security and service-layer checks.

---

## Operational Notes

- Outbox processing runs inside the application process.
- Event consumers must handle replay and duplicates safely.
- External dependency latency affects `POST /payments` directly.

---

## Summary

`payment-service` is the payment write-model for the system. It owns payment state, exposes read endpoints for payment data, and publishes payment outcome events for downstream systems through an asynchronous outbox flow.
