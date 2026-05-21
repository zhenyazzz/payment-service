package com.innowise.paymentservice.dto.request;

import com.innowise.paymentservice.model.enums.PaymentStatus;
import com.innowise.paymentservice.validation.annotation.ValidPaymentFilter;


@ValidPaymentFilter
public record PaymentSearchFilter(
        String userId,
        String orderId,
        PaymentStatus status
) {
}
