package com.innowise.paymentservice.repository;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.innowise.paymentservice.model.Payment;

public interface PaymentRepository extends MongoRepository<Payment, String> {

}
