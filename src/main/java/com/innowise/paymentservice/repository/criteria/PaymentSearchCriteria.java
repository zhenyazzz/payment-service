package com.innowise.paymentservice.repository.criteria;

import com.innowise.paymentservice.model.enums.PaymentStatus;

public record PaymentSearchCriteria(
    String userId,
    String orderId,
    PaymentStatus status
) {}
