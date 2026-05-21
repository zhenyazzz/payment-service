package com.innowise.paymentservice.config;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;

import io.netty.channel.ChannelOption;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;

@Configuration
public class WebClientConfig {
    private static final int MAX_CONNECTIONS = 50;
    private static final int CONNECT_TIMEOUT_MILLIS = 5000;
    private static final int PENDING_ACQUIRE_TIMEOUT_SECONDS = 5;
    private static final int RESPONSE_TIMEOUT_SECONDS = 5;

    @Bean("orderServiceWebClient")
    public WebClient orderServiceWebClient(
            @Value("${external.order-service.url}") String baseUrl) {
        return buildWebClient(baseUrl, "order-service-client");
    }

    @Bean("randomOrgWebClient")
    public WebClient randomOrgWebClient(
            @Value("${external.random-api.url}") String baseUrl) {
        return buildWebClient(baseUrl, "random-org-client");
    }

    private WebClient buildWebClient(String baseUrl, String clientName) {
        ConnectionProvider provider = ConnectionProvider.builder(clientName)
            .maxConnections(MAX_CONNECTIONS)
            .pendingAcquireTimeout(Duration.ofSeconds(PENDING_ACQUIRE_TIMEOUT_SECONDS))
            .build();

        HttpClient httpClient = HttpClient.create(provider)
            .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, CONNECT_TIMEOUT_MILLIS)
            .responseTimeout(Duration.ofSeconds(RESPONSE_TIMEOUT_SECONDS));

        return WebClient.builder()
            .baseUrl(baseUrl)
            .clientConnector(new ReactorClientHttpConnector(httpClient))
            .build();
    }
}