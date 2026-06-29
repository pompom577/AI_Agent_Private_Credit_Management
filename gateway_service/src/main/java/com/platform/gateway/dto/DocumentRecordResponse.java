package com.platform.gateway.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record DocumentRecordResponse(
        @JsonProperty("id")         Long id,
        @JsonProperty("filename")   String filename,
        @JsonProperty("category")   String category,
        @JsonProperty("status")     String status,
        @JsonProperty("page_count") Integer pageCount) {
}
