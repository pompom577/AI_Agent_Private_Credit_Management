package com.platform.gateway.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Success payload returned by POST /uploads (201 Created).
 *
 * Snake_case wire contract — coordinated with Person 1 (Frontend) and Person 3 (FastAPI).
 */
public record UploadResponse(
        @JsonProperty("bucket_url") String bucketUrl,
        @JsonProperty("deal_id") String dealId) {
}
