package com.innowise.paymentservice.client.impl;

import java.math.BigDecimal;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import com.innowise.paymentservice.client.OrderClient;
import com.innowise.paymentservice.dto.internal.InternalOrderPriceResponse;
import com.innowise.paymentservice.exception.notfound.OrderNotFoundException;

import lombok.RequiredArgsConstructor;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;

@Component
@RequiredArgsConstructor
public class WebClientOrderClient implements OrderClient {

    @Qualifier("orderServiceWebClient")
    private final WebClient orderServiceWebClient;

    @Override
    @CircuitBreaker(name = "orderService")
    @Retry(name = "orderService")
    public BigDecimal getOrderTotalPrice(String orderId, String userId) {
        try {
            InternalOrderPriceResponse orderPriceResponse = orderServiceWebClient.get()
                .uri(uriBuilder -> uriBuilder.path("/orders/internal/{id}/total-price").build(orderId))
                .header("X-User-Id", userId)
                .retrieve()
                .bodyToMono(InternalOrderPriceResponse.class)
                .block();

            if (orderPriceResponse == null || orderPriceResponse.totalPrice() == null) {
                throw new OrderNotFoundException("Order not found for orderId: " + orderId);
            }

            return orderPriceResponse.totalPrice();
        } catch (WebClientResponseException.NotFound e) {
            throw new OrderNotFoundException("Order not found for orderId: " + orderId);
        }
    }

}
