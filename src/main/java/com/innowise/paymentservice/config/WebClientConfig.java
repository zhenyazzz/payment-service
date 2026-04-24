package com.innowise.paymentservice.config;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import reactor.netty.http.client.HttpClient;

@Configuration
public class WebClientConfig {

    private ReactorClientHttpConnector clientHttpConnector() {
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000)
                .responseTimeout(Duration.ofSeconds(5))
                .doOnConnected(conn -> conn
                        .addHandlerLast(new ReadTimeoutHandler(5))
                        .addHandlerLast(new WriteTimeoutHandler(5)));
        return new ReactorClientHttpConnector(httpClient);
    }    

    @Bean("randomOrgWebClient")
    public WebClient randomOrgWebClient(
            @Value("${external.random-api.url:https://www.random.org}") String baseUrl) {

        return WebClient.builder()
                .clientConnector(clientHttpConnector())
                .baseUrl(baseUrl)
                .build();
    }
}