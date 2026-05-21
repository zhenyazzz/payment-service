package com.innowise.paymentservice.repository;

import java.math.BigDecimal;
import java.time.Instant;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.innowise.paymentservice.model.Payment;
import com.innowise.paymentservice.repository.criteria.PaymentSearchCriteria;
import com.innowise.paymentservice.repository.criteria.AdvancedPaymentSearchCriteria;

public interface PaymentRepositoryCustom {
    BigDecimal sumByUserIdDateRange(String userId, Instant from, Instant to);
    BigDecimal sumAllPaymentsDateRange(Instant from, Instant to);
    Page<Payment> findPaymentsByCriteria(PaymentSearchCriteria criteria, Pageable pageable);
    Page<Payment> searchPaymentsByCriteria(AdvancedPaymentSearchCriteria criteria, Pageable pageable);
}
