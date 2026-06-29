package com.platform.gateway.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Body sent from Gateway -> FastAPI POST /classify (Sub-Story 1.1c).
 */
public record ClassifyRequest(
        @JsonProperty("bucket_url") String bucketUrl,
        @JsonProperty("deal_id") String dealId,
        @JsonProperty("uploaded_by_user_id") String uploadedByUserId) {
}
