package com.innowise.paymentservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record CreatePaymentRequest(
        @NotBlank(message = "Order ID is required")
        String orderId,

        @NotNull(message = "Payment amount is required")
        @Positive(message = "Payment amount must be greater than 0")
        BigDecimal paymentAmount
) {
}
