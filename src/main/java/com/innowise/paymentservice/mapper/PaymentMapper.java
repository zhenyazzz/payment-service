package com.innowise.paymentservice.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.innowise.paymentservice.model.Payment;

import com.innowise.paymentservice.dto.request.CreatePaymentRequest;
import com.innowise.paymentservice.dto.response.PaymentResponse;
import com.innowise.paymentservice.producer.PaymentCreatedEvent;

@Mapper(componentModel = "spring")
public interface PaymentMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "userId", source = "userId")
    @Mapping(target = "status", expression = "java(PaymentStatus.PENDING)")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Payment toEntity(CreatePaymentRequest request, String userId);

    PaymentResponse toResponse(Payment payment);

    @Mapping(target = "paymentId", source = "id")
    @Mapping(target = "orderId", source = "orderId")
    @Mapping(target = "userId", source = "userId")
    @Mapping(target = "status", source = "status")
    PaymentCreatedEvent toPaymentCreatedEvent(Payment payment);
}
