package com.innowise.paymentservice.validation.validator;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import com.innowise.paymentservice.dto.request.AdvancedPaymentSearchFilter;
import com.innowise.paymentservice.validation.annotation.ValidDateRange;

public class DateRangeValidator implements ConstraintValidator<ValidDateRange, AdvancedPaymentSearchFilter> {

    @Override
    public boolean isValid(AdvancedPaymentSearchFilter value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }

        if (value.createdFrom() == null && value.createdTo() == null) {
            return true;
        }

        if (value.createdFrom() == null || value.createdTo() == null) {
            return false;
        }

        if (value.createdFrom().isAfter(value.createdTo())) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(
                    "Created from must not be after created to date")
                    .addPropertyNode("createdTo")
                    .addConstraintViolation();
            return false;
        }

        return true;
    }
}
