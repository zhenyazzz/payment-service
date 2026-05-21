package com.innowise.paymentservice.repository.criteria;

import java.time.Instant;
import java.util.List;

import com.innowise.paymentservice.model.enums.PaymentStatus;

public record AdvancedPaymentSearchCriteria(
    String userId,
    String orderId,
    List<PaymentStatus> statuses,
    Instant createdFrom,
    Instant createdTo
) {}
