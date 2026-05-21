package com.innowise.paymentservice.exception.external;

public class RandomOrgUnavailableException extends PaymentProviderUnavailableException {

    public RandomOrgUnavailableException(String message) {
        super(message);
    }

    public RandomOrgUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
