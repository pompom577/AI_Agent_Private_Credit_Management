package com.platform.gateway.clients;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.platform.gateway.dto.ClassifyRequest;
import com.platform.gateway.exceptions.ClassificationHandoffException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.WebClient;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * WireMock-backed integration tests for {@link FastApiClient} — covers TC-GW-06
 * (Gateway -> FastAPI /classify handoff) without bringing up the full Spring context.
 *
 * Asserted contract:
 *   - POST /classify
 *   - Authorization: Bearer &lt;jwt&gt;
 *   - Body has bucket_url, deal_id, uploaded_by_user_id (snake_case)
 *   - 202 -> success (no throw)
 *   - 401 / 422 -> ClassificationHandoffException with that exact upstream status
 */
class FastApiClientTest {

    private static WireMockServer wireMock;

    private FastApiClient client;
    private final ClassifyRequest sampleRequest =
            new ClassifyRequest("s3://test-bucket/deal-1/file.zip", "deal-1", "user-42");

    @BeforeAll
    static void startWireMock() {
        wireMock = new WireMockServer(options().dynamicPort());
        wireMock.start();
    }

    @AfterAll
    static void stopWireMock() {
        wireMock.stop();
    }

    @BeforeEach
    void wireClientToWireMock() {
        wireMock.resetAll();
        WebClient wc = WebClient.builder()
                .baseUrl("http://localhost:" + wireMock.port())
                .build();
        client = new FastApiClient(wc);
    }

    @Test
    void tcGw06_postsBearerJwtAndJsonBody_andSucceedsOn202() {
        wireMock.stubFor(post(urlEqualTo("/classify"))
                .withHeader("Authorization", equalTo("Bearer fake-jwt"))
                .withHeader("Content-Type", equalTo("application/json"))
                .withRequestBody(matchingJsonPath("$.bucket_url"))
                .withRequestBody(matchingJsonPath("$.deal_id"))
                .withRequestBody(matchingJsonPath("$.uploaded_by_user_id"))
                .willReturn(aResponse().withStatus(202)));

        client.classify("fake-jwt", sampleRequest);

        wireMock.verify(postRequestedFor(urlEqualTo("/classify"))
                .withHeader("Authorization", equalTo("Bearer fake-jwt")));
    }

    @Test
    void tcGw06_propagatesUpstream401() {
        wireMock.stubFor(post(urlEqualTo("/classify"))
                .willReturn(aResponse().withStatus(401)));

        assertThatThrownBy(() -> client.classify("bad-jwt", sampleRequest))
                .isInstanceOfSatisfying(ClassificationHandoffException.class, ex ->
                        assertThat(ex.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED));
    }

    @Test
    void tcGw06_propagatesUpstream422() {
        wireMock.stubFor(post(urlEqualTo("/classify"))
                .willReturn(aResponse().withStatus(422)));

        assertThatThrownBy(() -> client.classify("ok-jwt", sampleRequest))
                .isInstanceOfSatisfying(ClassificationHandoffException.class, ex ->
                        assertThat(ex.getStatus()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY));
    }

    @Test
    void transportFailure_mapsTo502BadGateway() {
        WebClient deadClient = WebClient.builder()
                .baseUrl("http://localhost:1") // unroutable
                .build();
        FastApiClient unreachable = new FastApiClient(deadClient);

        assertThatThrownBy(() -> unreachable.classify("ok-jwt", sampleRequest))
                .isInstanceOfSatisfying(ClassificationHandoffException.class, ex ->
                        assertThat(ex.getStatus()).isEqualTo(HttpStatus.BAD_GATEWAY));
    }
}
