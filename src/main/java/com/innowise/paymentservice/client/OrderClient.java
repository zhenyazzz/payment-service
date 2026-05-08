package com.innowise.paymentservice.client;

import java.math.BigDecimal;

public interface OrderClient {

    BigDecimal getOrderTotalPrice(String orderId, String userId);
}
