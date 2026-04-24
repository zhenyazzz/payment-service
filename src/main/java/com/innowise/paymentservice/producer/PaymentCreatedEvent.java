package com.innowise.paymentservice.producer;

import com.innowise.paymentservice.model.enums.PaymentStatus;

public record PaymentCreatedEvent(
    String paymentId,
    String orderId,
    String userId,
    PaymentStatus status
) {

}
