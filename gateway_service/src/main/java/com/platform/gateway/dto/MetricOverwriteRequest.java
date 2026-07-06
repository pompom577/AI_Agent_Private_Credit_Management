package com.platform.gateway.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;

public class MetricOverwriteRequest {

    @NotBlank
    @JsonProperty("new_value")
    private String newValue;

    @NotBlank
    @JsonProperty("user_id")
    private String userId;

    public MetricOverwriteRequest() {}

    public String getNewValue() { return newValue; }
    public void setNewValue(String newValue) { this.newValue = newValue; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
}
