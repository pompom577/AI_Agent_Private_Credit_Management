package com.platform.gateway;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Skeleton-phase smoke test: boots the full Spring context, verifies all @Configuration
 * classes wire cleanly with the test-profile placeholders, and asserts the actuator
 * health endpoint returns UP.
 *
 * Once 1.1a / 1.1b / 1.1c are implemented, dedicated MockMvc / WireMock tests for
 * TC-GW-01..06 will live alongside this one.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class GatewayApplicationSmokeTest {

    @LocalServerPort
    int port;

    @Autowired
    TestRestTemplate rest;

    @Test
    void contextLoadsAndHealthEndpointIsUp() {
        ResponseEntity<String> response =
                rest.getForEntity("http://localhost:" + port + "/actuator/health", String.class);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).contains("\"status\":\"UP\"");
    }
}
