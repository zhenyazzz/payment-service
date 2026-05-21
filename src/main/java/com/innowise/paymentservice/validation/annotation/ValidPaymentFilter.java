package com.innowise.paymentservice.validation.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import com.innowise.paymentservice.validation.validator.PaymentFilterValidator;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = PaymentFilterValidator.class)
@Documented
public @interface ValidPaymentFilter {
    String message() default "Need to provide only one of the following parameters: userId, orderId, status";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
