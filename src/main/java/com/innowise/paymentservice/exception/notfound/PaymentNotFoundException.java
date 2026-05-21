package com.innowise.paymentservice.exception.notfound;

public class PaymentNotFoundException extends ResourceNotFoundException {

    public PaymentNotFoundException(String message) {
        super(message);
    }
}
