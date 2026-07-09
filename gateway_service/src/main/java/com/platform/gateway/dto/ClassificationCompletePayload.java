package com.platform.gateway.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Inbound webhook payload from FastAPI classification service
 * (POST /webhooks/classification-complete, Story 1.2, Person 3).
 */
public class ClassificationCompletePayload {

    @NotBlank
    @JsonProperty("deal_id")
    private String dealId;

    @NotBlank
    @JsonProperty("status")
    private String status;

    @NotNull
    @JsonProperty("total_documents")
    private Integer totalDocuments;

    public ClassificationCompletePayload() {}

    public String getDealId() { return dealId; }
    public void setDealId(String dealId) { this.dealId = dealId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Integer getTotalDocuments() { return totalDocuments; }
    public void setTotalDocuments(Integer totalDocuments) { this.totalDocuments = totalDocuments; }
}
