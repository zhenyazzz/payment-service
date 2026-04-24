package com.innowise.paymentservice.validation.validator;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.Objects;
import java.util.stream.Stream;

import com.innowise.paymentservice.dto.request.PaymentSearchFilter;
import com.innowise.paymentservice.validation.annotation.ValidPaymentFilter;

public class PaymentFilterValidator implements ConstraintValidator<ValidPaymentFilter, PaymentSearchFilter> {

    @Override
    public boolean isValid(PaymentSearchFilter filter, ConstraintValidatorContext context) {
        if (filter == null) {
            return false;
        }

        long count = Stream.of(
            filter.userId(), 
            filter.orderId(), 
            filter.status())
            .filter(Objects::nonNull)
            .count();

        return count == 1;
    }

}
