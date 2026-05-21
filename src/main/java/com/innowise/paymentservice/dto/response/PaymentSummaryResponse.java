package com.innowise.paymentservice.dto.response;

import java.math.BigDecimal;
import java.time.Instant;

public record PaymentSummaryResponse(
    BigDecimal totalAmount,
    Instant startDate,
    Instant endDate
) {
}
