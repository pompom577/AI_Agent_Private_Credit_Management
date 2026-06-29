package com.platform.gateway.dto;

/**
 * Uniform error body for 415 / 422 / 5xx responses from the Gateway.
 * Matches the contract defined in the 1.1b sequence diagram: {@code {reason: "..."}}.
 */
public record ErrorResponse(String reason) {
}
