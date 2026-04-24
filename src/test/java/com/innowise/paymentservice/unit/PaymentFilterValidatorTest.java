package com.innowise.paymentservice.unit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.innowise.paymentservice.dto.request.PaymentSearchFilter;
import com.innowise.paymentservice.model.enums.PaymentStatus;
import com.innowise.paymentservice.validation.validator.PaymentFilterValidator;

import jakarta.validation.ConstraintValidatorContext;

class PaymentFilterValidatorTest {

    private PaymentFilterValidator validator;

    private ConstraintValidatorContext context;

    @BeforeEach
    void setUp() {
        validator = new PaymentFilterValidator();
        context = mock(ConstraintValidatorContext.class);
    }

    @Test
    void nullFilter_invalid() {
        assertThat(validator.isValid(null, context)).isFalse();
    }

    @Test
    void noCriterion_invalid() {
        var filter = new PaymentSearchFilter(null, null, null);
        assertThat(validator.isValid(filter, context)).isFalse();
    }

    @Test
    void onlyUserId_valid() {
        var filter = new PaymentSearchFilter(UUID.randomUUID().toString(), null, null);
        assertThat(validator.isValid(filter, context)).isTrue();
    }

    @Test
    void onlyOrderId_valid() {
        var filter = new PaymentSearchFilter(null, "order-1", null);
        assertThat(validator.isValid(filter, context)).isTrue();
    }

    @Test
    void onlyStatus_valid() {
        var filter = new PaymentSearchFilter(null, null, PaymentStatus.SUCCESS);
        assertThat(validator.isValid(filter, context)).isTrue();
    }

    @Test
    void twoCriteria_invalid() {
        var filter = new PaymentSearchFilter(UUID.randomUUID().toString(), "order-1", null);
        assertThat(validator.isValid(filter, context)).isFalse();
    }

    @Test
    void allThreeCriteria_invalid() {
        var filter = new PaymentSearchFilter(
            UUID.randomUUID().toString(),
            "order-1",
            PaymentStatus.FAILED
        );
        assertThat(validator.isValid(filter, context)).isFalse();
    }
}
