package com.innowise.paymentservice.client;

import java.math.BigDecimal;

/**
 * Client contract for resolving order data needed by the payment workflow.
 */
public interface OrderClient {

    /**
     * Returns the current total price of the order after validating access for the caller.
     *
     * @param orderId order identifier
     * @param userId current authenticated user identifier
     * @return order total price
     */
    BigDecimal getOrderTotalPrice(String orderId, String userId);
}
