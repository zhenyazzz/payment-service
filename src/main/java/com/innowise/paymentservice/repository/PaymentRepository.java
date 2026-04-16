package com.innowise.paymentservice.repository;

import java.math.BigDecimal;
import java.time.Instant;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import com.innowise.paymentservice.model.Payment;
import com.innowise.paymentservice.model.enums.PaymentStatus;

public interface PaymentRepository extends MongoRepository<Payment, String>, PaymentRepositoryCustom {

    Page<Payment> findByUserId(String userId, Pageable pageable);

    Page<Payment> findByOrderId(String orderId, Pageable pageable);

    Page<Payment> findByStatus(PaymentStatus status, Pageable pageable);

    BigDecimal sumByUserId(String userId, Instant from, Instant to);
}
