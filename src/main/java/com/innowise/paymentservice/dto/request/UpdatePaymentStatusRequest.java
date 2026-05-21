package com.innowise.paymentservice.dto.request;

import com.innowise.paymentservice.model.enums.PaymentStatus;

import jakarta.validation.constraints.NotNull;

public record UpdatePaymentStatusRequest(
        @NotNull(message = "Status is required")
        PaymentStatus status
) {
}
