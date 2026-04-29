package com.innowise.paymentservice.client.impl;

import com.innowise.paymentservice.client.AcquiringResult;
import com.innowise.paymentservice.client.PaymentAcquiringClient;
import com.innowise.paymentservice.exception.external.RandomOrgUnavailableException;

import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.reactive.function.client.WebClient;

import lombok.RequiredArgsConstructor;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class RandomOrgAcquiringAdapter implements PaymentAcquiringClient {

    @Qualifier("randomOrgWebClient")
    private final WebClient randomOrgWebClient;

    @Override
    @Retry(name = "randomOrgAcquiring", fallbackMethod = "fallbackTransaction")
    public AcquiringResult getAcquiringResult() {
        String responseBody = randomOrgWebClient.get()
                .uri(uriBuilder -> uriBuilder
                    .path("/integers/")
                    .queryParam("num", 1)
                    .queryParam("min", 1)
                    .queryParam("max", 100)
                    .queryParam("col", 1)
                    .queryParam("base", 10)
                    .queryParam("format", "plain")
                    .queryParam("rnd", "new")
                    .build()
                )
                .retrieve()
                .bodyToMono(String.class)
                .block();

        if (responseBody == null || responseBody.isBlank()) {
            throw new RandomOrgUnavailableException("Empty response from Random.org");
        }

        int resultNumber = Integer.parseInt(responseBody.trim());

        return new AcquiringResult(resultNumber);
    }

    public AcquiringResult fallbackTransaction(Exception e) {
        log.error("Payment gateway is unavailable after all attempts! Reason: {}", e.getMessage());
        throw new RandomOrgUnavailableException("Failed to connect to Random.org", e);
    }

}
