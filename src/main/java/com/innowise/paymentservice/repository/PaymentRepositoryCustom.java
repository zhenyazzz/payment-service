package com.innowise.paymentservice.repository;

import java.math.BigDecimal;
import java.time.Instant;

public interface PaymentRepositoryCustom {
    BigDecimal sumByUserIdDateRange(String userId, Instant from, Instant to);
    BigDecimal sumAllPaymentsDateRange(Instant from, Instant to);
}
