package com.innowise.paymentservice.dto.request;

import jakarta.validation.constraints.NotBlank;

public record CreatePaymentRequest(
        @NotBlank(message = "Order ID is required")
        String orderId
) {
}
