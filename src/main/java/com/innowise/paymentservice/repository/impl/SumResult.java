package com.innowise.paymentservice.repository.impl;

import java.math.BigDecimal;

public record SumResult(
    BigDecimal totalAmount
) {}
