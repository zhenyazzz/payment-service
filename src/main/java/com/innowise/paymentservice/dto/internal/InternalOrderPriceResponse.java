package com.innowise.paymentservice.dto.internal;

import java.math.BigDecimal;

public record InternalOrderPriceResponse(
    BigDecimal totalPrice
) {

}
