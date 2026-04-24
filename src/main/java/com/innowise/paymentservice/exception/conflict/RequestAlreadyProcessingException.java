package com.innowise.paymentservice.exception.conflict;

public class RequestAlreadyProcessingException extends RuntimeException {

    public RequestAlreadyProcessingException(String message) {
        super(message);
    }
}
