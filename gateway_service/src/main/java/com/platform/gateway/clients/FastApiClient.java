package com.platform.gateway.clients;

import com.platform.gateway.dto.ClassifyRequest;
import com.platform.gateway.exceptions.ClassificationHandoffException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;

/**
 * HTTP client for the FastAPI /classify microservice (1.1c handoff).
 *
 * Wire contract:
 *   POST /classify
 *   Authorization: Bearer &lt;JWT minted by JwtService&gt;
 *   Content-Type: application/json
 *   Body: {bucket_url, deal_id, uploaded_by_user_id}
 *
 * Behaviour:
 *   2xx (typically 202 Accepted) -> return normally.
 *   non-2xx                       -> throw ClassificationHandoffException with upstream status.
 *   transport failure             -> throw ClassificationHandoffException with 502 Bad Gateway.
 */
@Component
public class FastApiClient {

    private static final Logger log = LoggerFactory.getLogger(FastApiClient.class);

    private final WebClient classificationWebClient;

    public FastApiClient(WebClient classificationWebClient) {
        this.classificationWebClient = classificationWebClient;
    }

    public void classify(String bearerJwt, ClassifyRequest body) {
        try {
            classificationWebClient.post()
                    .uri("/classify")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + bearerJwt)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(body)
                    .retrieve()
                    .toBodilessEntity()
                    .block();
            log.info("Classification handoff accepted for deal {}", body.dealId());
        } catch (WebClientResponseException e) {
            log.warn("Classification handoff rejected for deal {}: status={}",
                    body.dealId(), e.getStatusCode());
            throw new ClassificationHandoffException(e.getStatusCode(), e.getMessage());
        } catch (WebClientRequestException e) {
            log.error("Classification service unreachable for deal {}: {}", body.dealId(), e.getMessage());
            throw new ClassificationHandoffException(
                    HttpStatus.BAD_GATEWAY,
                    "classification service unreachable: " + e.getMessage());
        }
    }
}
