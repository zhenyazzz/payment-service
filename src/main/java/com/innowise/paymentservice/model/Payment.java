package com.innowise.paymentservice.model;

import java.math.BigDecimal;
import java.time.Instant;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;

import com.innowise.paymentservice.model.enums.PaymentStatus;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@CompoundIndex(name = "user_date_idx", def = "{'userId': 1, 'createdAt': 1}")
@Document(collection = "payments")
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
