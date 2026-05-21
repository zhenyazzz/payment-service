package com.innowise.paymentservice.exception.external;

public abstract class PaymentProviderUnavailableException extends RuntimeException {

    protected PaymentProviderUnavailableException(String message) {
        super(message);
    }

    protected PaymentProviderUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
