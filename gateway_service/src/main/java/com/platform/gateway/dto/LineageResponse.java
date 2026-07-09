package com.platform.gateway.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record LineageResponse(
        @JsonProperty("metric_id")    Long metricId,
        @JsonProperty("metric_name")  String metricName,
        @JsonProperty("raw_value")    String rawValue,
        @JsonProperty("source_doc_id") Long sourceDocId,
        @JsonProperty("page_number")  Integer pageNumber,
        @JsonProperty("bbox")         List<Double> bbox) {
}
