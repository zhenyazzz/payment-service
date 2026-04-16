package com.innowise.paymentservice.repository.impl;

import java.math.BigDecimal;
import java.time.Instant;

import org.bson.Document;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.aggregation.MatchOperation;
import org.springframework.stereotype.Repository;

import com.innowise.paymentservice.model.Payment;
import com.innowise.paymentservice.repository.PaymentRepositoryCustom;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class PaymentRepositoryImpl implements PaymentRepositoryCustom {
    private final MongoTemplate mongoTemplate;

    @Override
    public BigDecimal sumByUserIdDateRange(String userId, Instant from, Instant to) {
        Criteria criteria = Criteria.where("userId").is(userId)
                    .and("createdAt").gte(from).lte(to);

        Aggregation aggregation = buildAggregation(criteria);

        return extractResult(aggregation);
    }

    @Override
    public BigDecimal sumAllPaymentsDateRange(Instant from, Instant to) {
        Criteria criteria = Criteria.where("createdAt").gte(from).lte(to);
        
        Aggregation aggregation = buildAggregation(criteria);

        return extractResult(aggregation);
    }

    private Aggregation buildAggregation(Criteria criteria) {
        return Aggregation.newAggregation(
            Aggregation.match(criteria),
            Aggregation.group().sum("paymentAmount").as("totalAmount")
        );
    }

    private BigDecimal extractResult(Aggregation aggregation){
        AggregationResults<SumResult> results = mongoTemplate.aggregate(
            aggregation, Payment.class, SumResult.class
        );
        
        SumResult sumResult = results.getUniqueMappedResult();

        return sumResult != null
            ? sumResult.getTotalAmount()
            : BigDecimal.ZERO;
    }

}
