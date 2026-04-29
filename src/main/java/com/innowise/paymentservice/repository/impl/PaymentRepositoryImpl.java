package com.innowise.paymentservice.repository.impl;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.stereotype.Repository;

import com.innowise.paymentservice.model.Payment;
import com.innowise.paymentservice.model.enums.PaymentStatus;
import com.innowise.paymentservice.repository.PaymentRepositoryCustom;
import com.innowise.paymentservice.repository.criteria.PaymentSearchCriteria;
import com.innowise.paymentservice.repository.criteria.AdvancedPaymentSearchCriteria;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class PaymentRepositoryImpl implements PaymentRepositoryCustom {

    private static final String FIELD_USER_ID = "userId";
    private static final String FIELD_ORDER_ID = "orderId";
    private static final String FIELD_STATUS = "status";
    private static final String FIELD_CREATED_AT = "createdAt";
    private static final String FIELD_PAYMENT_AMOUNT = "paymentAmount";
    private static final String ALIAS_TOTAL_AMOUNT = "totalAmount";

    private final MongoTemplate mongoTemplate;

    @Override
    public BigDecimal sumByUserIdDateRange(String userId, Instant from, Instant to) {
        Criteria criteria = Criteria.where(FIELD_USER_ID).is(userId)
                    .and(FIELD_CREATED_AT).gte(from).lte(to)
                    .and(FIELD_STATUS).is(PaymentStatus.SUCCESS);

        Aggregation aggregation = buildAggregation(criteria);

        return extractResult(aggregation);
    }

    @Override
    public BigDecimal sumAllPaymentsDateRange(Instant from, Instant to) {
        Criteria criteria = Criteria.where(FIELD_CREATED_AT).gte(from).lte(to)
        .and(FIELD_STATUS).is(PaymentStatus.SUCCESS);
        
        Aggregation aggregation = buildAggregation(criteria);

        return extractResult(aggregation);
    }

    private Aggregation buildAggregation(Criteria criteria) {
        return Aggregation.newAggregation(
            Aggregation.match(criteria),
            Aggregation.group().sum(FIELD_PAYMENT_AMOUNT).as(ALIAS_TOTAL_AMOUNT)
        );
    }

    private BigDecimal extractResult(Aggregation aggregation){
        AggregationResults<SumResult> results = mongoTemplate.aggregate(
            aggregation, Payment.class, SumResult.class
        );
        
        SumResult sumResult = results.getUniqueMappedResult();

        return sumResult != null
            ? sumResult.totalAmount()
            : BigDecimal.ZERO;
    }

    @Override
    public Page<Payment> findPaymentsByCriteria(PaymentSearchCriteria criteria, Pageable pageable) {
        Query query = new Query();

        if (criteria.userId() != null) {
            query.addCriteria(Criteria.where(FIELD_USER_ID).is(criteria.userId()));
        }
        if (criteria.orderId() != null) {
            query.addCriteria(Criteria.where(FIELD_ORDER_ID).is(criteria.orderId()));
        }
        if (criteria.status() != null) {
            query.addCriteria(Criteria.where(FIELD_STATUS).is(criteria.status()));
        }

        return getPaginatedResult(query, pageable);
    }

    @Override
    public Page<Payment> searchPaymentsByCriteria(AdvancedPaymentSearchCriteria criteria, Pageable pageable) {
        Query query = new Query();

        if (criteria.userId() != null) {
            query.addCriteria(Criteria.where(FIELD_USER_ID).is(criteria.userId()));
        }
        if (criteria.orderId() != null) {
            query.addCriteria(Criteria.where(FIELD_ORDER_ID).is(criteria.orderId()));
        }
        if (criteria.statuses() != null && !criteria.statuses().isEmpty()) {
            query.addCriteria(Criteria.where(FIELD_STATUS).in(criteria.statuses()));
        }
        if (criteria.createdFrom() != null || criteria.createdTo() != null) {
            Criteria dateCriteria = Criteria.where(FIELD_CREATED_AT);
            if (criteria.createdFrom() != null) {
                dateCriteria.gte(criteria.createdFrom());
            }
            if (criteria.createdTo() != null) {
                dateCriteria.lte(criteria.createdTo());
            }
            query.addCriteria(dateCriteria);
        }

        return getPaginatedResult(query, pageable);
    }

    private Page<Payment> getPaginatedResult(Query query, Pageable pageable) {
        Query countQuery = Query.of(query); 

        query.with(pageable);

        List<Payment> payments = mongoTemplate.find(query, Payment.class);

        return PageableExecutionUtils.getPage(
            payments, 
            pageable, 
            () -> mongoTemplate.count(countQuery, Payment.class)
        );
    }

}
