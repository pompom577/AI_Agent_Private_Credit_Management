package com.platform.gateway.exceptions;

import org.springframework.http.HttpStatusCode;

/**
 * Raised by {@code FastApiClient.classify} when the downstream classification
 * service responds with a non-2xx status or is unreachable. {@link GlobalExceptionHandler}
 * propagates the upstream status code (and a sanitized reason) back to the original
 * caller per the 1.1c contract.
 *
 * Covers TC-GW-06 (relay 202 / 401 / 422 to caller).
 */
public class ClassificationHandoffException extends RuntimeException {

    private final HttpStatusCode status;

    public ClassificationHandoffException(HttpStatusCode status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatusCode getStatus() {
        return status;
    }
}
