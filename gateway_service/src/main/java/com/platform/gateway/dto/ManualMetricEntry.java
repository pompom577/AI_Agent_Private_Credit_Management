package com.platform.gateway.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;

public record ManualMetricEntry(
        @NotBlank @JsonProperty("metric_name") String metricName,
        @NotBlank @JsonProperty("raw_value")   String rawValue) {
}
