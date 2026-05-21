package com.innowise.paymentservice.dto.response;

import java.math.BigDecimal;
import java.time.Instant;

import com.innowise.paymentservice.model.enums.PaymentStatus;

public record PaymentResponse(
    String id,
    String orderId,
    String userId,
    PaymentStatus status,
    BigDecimal paymentAmount,
    Instant createdAt
) {}
