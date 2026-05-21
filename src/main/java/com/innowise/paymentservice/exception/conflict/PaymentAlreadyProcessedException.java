package com.innowise.paymentservice.exception.conflict;

public class PaymentAlreadyProcessedException extends RuntimeException {

    public PaymentAlreadyProcessedException(String message) {
        super(message);
    }
}
