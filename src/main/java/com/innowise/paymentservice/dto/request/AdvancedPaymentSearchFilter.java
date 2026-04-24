package com.innowise.paymentservice.dto.request;

import java.time.Instant;
import java.util.List;

import com.innowise.paymentservice.model.enums.PaymentStatus;
import com.innowise.paymentservice.validation.annotation.ValidDateRange;

@ValidDateRange
public record AdvancedPaymentSearchFilter(
    String userId,
    String orderId,
    List<PaymentStatus> statuses,
    Instant createdFrom,
    Instant createdTo
) {}
