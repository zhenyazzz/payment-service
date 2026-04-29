package com.innowise.paymentservice.repository;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.innowise.paymentservice.model.Payment;
import com.innowise.paymentservice.model.enums.PaymentStatus;

public interface PaymentRepository extends MongoRepository<Payment, String>, PaymentRepositoryCustom {
    boolean existsByOrderIdAndStatus(String orderId, PaymentStatus status);
}
