package com.innowise.paymentservice.utils;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import com.innowise.paymentservice.dto.request.AdvancedPaymentSearchFilter;
import com.innowise.paymentservice.dto.request.CreatePaymentRequest;
import com.innowise.paymentservice.dto.request.PaymentSearchFilter;
import com.innowise.paymentservice.model.Payment;
import com.innowise.paymentservice.model.enums.PaymentStatus;
import com.innowise.paymentservice.dto.response.PaymentResponse;
import com.innowise.paymentservice.producer.PaymentCreatedEvent;

import lombok.experimental.UtilityClass;

@UtilityClass
public class PaymentTestDataFactory {
    public static final String PAYMENT_ID = "payment-test-1";
    public static final String ORDER_ID = "order-test-1";
    public static final String USER_ID = "user-test-1";
    public static final String OTHER_USER_ID = "user-test-2";
    private static final BigDecimal PAYMENT_AMOUNT = new BigDecimal("10.00");
    private static final Instant FIXED_INSTANT = Instant.parse("2025-06-01T12:00:00Z");

    public Payment buildPayment() {
        return buildPayment(PaymentStatus.PENDING);
    }

    public Payment buildPayment(PaymentStatus status) {
        return buildPayment(PAYMENT_ID, ORDER_ID, USER_ID, status);
    }

    public Payment buildPayment(PaymentStatus status, BigDecimal paymentAmount, Instant createdAt) {
        return buildPayment(PAYMENT_ID, ORDER_ID, USER_ID, status, paymentAmount, createdAt);
    }

    public Payment buildPayment(
        String paymentId,
        String orderId,
        String userId,
        PaymentStatus status
    ) {
        Payment p = new Payment();
        p.setId(paymentId);
        p.setOrderId(orderId);
        p.setUserId(userId);
        p.setStatus(status);
        p.setPaymentAmount(PAYMENT_AMOUNT);
        p.setCreatedAt(FIXED_INSTANT);
        p.setUpdatedAt(FIXED_INSTANT);
        return p;
    }

    public Payment buildPayment(
        String paymentId,
        String orderId,
        String userId,
        PaymentStatus status,
        BigDecimal paymentAmount,
        Instant createdAt
    ) {
        Payment p = buildPayment(paymentId, orderId, userId, status);
        p.setPaymentAmount(paymentAmount);
        p.setCreatedAt(createdAt);
        p.setUpdatedAt(createdAt);
        return p;
    }

    public PaymentCreatedEvent buildPaymentCreatedEvent() {
        return buildPaymentCreatedEvent(PaymentStatus.SUCCESS);
    }

    public PaymentCreatedEvent buildPaymentCreatedEvent(PaymentStatus status) {
        return buildPaymentCreatedEvent(PAYMENT_ID, ORDER_ID, USER_ID, status);
    }

    public PaymentCreatedEvent buildPaymentCreatedEvent(
        String paymentId,
        String orderId,
        String userId,
        PaymentStatus status
    ) {
        return new PaymentCreatedEvent(paymentId, orderId, userId, status);
    }

    public CreatePaymentRequest buildCreatePaymentRequest() {
        return new CreatePaymentRequest(ORDER_ID, PAYMENT_AMOUNT);
    }

    public PaymentSearchFilter buildPaymentSearchFilter() {
        return new PaymentSearchFilter(USER_ID, ORDER_ID, PaymentStatus.SUCCESS);
    }

    public AdvancedPaymentSearchFilter buildAdvancedPaymentSearchFilter() {
        return new AdvancedPaymentSearchFilter(
            USER_ID,
            ORDER_ID,
            List.of(PaymentStatus.SUCCESS, PaymentStatus.FAILED),
            FIXED_INSTANT.minusSeconds(3600),
            FIXED_INSTANT.plusSeconds(3600)
        );
    }

    public PaymentResponse buildPaymentResponse() {
        return buildPaymentResponse(buildPayment());
    }

    public PaymentResponse buildPaymentResponse(Payment payment) {
        return new PaymentResponse(
            payment.getId(),
            payment.getOrderId(),
            payment.getUserId(),
            payment.getStatus(),
            payment.getPaymentAmount(),
            payment.getCreatedAt()
        );
    }
}
