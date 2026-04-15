package com.innowise.paymentservice.model;

import java.math.BigDecimal;
import java.time.Instant;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;

import com.innowise.paymentservice.model.enums.PaymentStatus;

import lombok.Getter;
import lombok.Setter;

@Document(collection = "payments")
@Getter
@Setter
public class Payment {

    @Id
    private String id;

    @Indexed
    private String orderId;

    @Indexed
    private String userId;

    @Indexed
    private PaymentStatus status;

    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal paymentAmount;

    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;
}
