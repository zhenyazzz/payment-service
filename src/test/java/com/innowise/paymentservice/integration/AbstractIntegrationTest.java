package com.innowise.paymentservice.integration;

import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;

import org.apache.kafka.clients.admin.NewTopic;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import com.github.tomakehurst.wiremock.WireMockServer;

@Testcontainers
@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public abstract class AbstractIntegrationTest {

    @LocalServerPort
    protected int port;

    protected WebTestClient webTestClient;

    protected static final WireMockServer wireMock = new WireMockServer(wireMockConfig().dynamicPort());

    @Container
    @ServiceConnection
    @SuppressWarnings("resource")
    static final GenericContainer<?> redis = new GenericContainer<>("redis:8")
        .withExposedPorts(6379);

    @Container
    @ServiceConnection
    static final MongoDBContainer mongo = new MongoDBContainer(
        DockerImageName.parse("mongo:8")
    );

    @Container
    static final KafkaContainer kafka = new KafkaContainer(
        DockerImageName.parse("apache/kafka-native:3.9.2")
    );

    @BeforeAll
    static void startWireMock() {
        wireMock.start();
    }

    @AfterAll
    static void stopWireMock() {
        wireMock.stop();
    }

    @BeforeEach
    void setUpWebTestClient() {
        wireMock.resetAll();
        this.webTestClient = WebTestClient.bindToServer()
            .baseUrl("http://localhost:" + port)
            .build();
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
        registry.add("external.random-api.url", wireMock::baseUrl);
    }

    protected void stubRandomOrgResponse(int responseCode) {
        wireMock.stubFor(get(urlEqualTo("/random/integer"))
            .willReturn(ok(String.valueOf(responseCode))));
    }

    protected void verifyRandomOrgCalled() {
        wireMock.verify(getRequestedFor(urlEqualTo("/random/integer")));
    }

    protected void verifyRandomOrgCalledTimes(int times) {
        wireMock.verify(times, getRequestedFor(urlEqualTo("/random/integer")));
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class KafkaTestConfig {
        @Bean
        NewTopic paymentEventsTopic(@Value("${kafka.topic.name:payment-events}") String topicName) {
            return TopicBuilder.name(topicName)
                .partitions(1)
                .replicas(1)
                .build();
        }
    }
}
