package com.innowise.paymentservice.producer;

import java.math.BigDecimal;
import java.time.Instant;

import com.innowise.paymentservice.model.enums.PaymentStatus;

public record CreatePaymentEvent(
    String paymentId,
    String orderId,
    String userId,
    BigDecimal paymentAmount,
    PaymentStatus status,
    Instant createdAt
) {

}
