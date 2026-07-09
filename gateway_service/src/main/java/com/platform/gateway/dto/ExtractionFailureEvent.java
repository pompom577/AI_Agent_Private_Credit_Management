package com.platform.gateway.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Inbound webhook payload from FastAPI extraction worker
 * (POST /webhooks/extraction-failed, Story 1.3, Person 3).
 */
public class ExtractionFailureEvent {

    @NotNull
    @JsonProperty("doc_id")
    private Long docId;

    @NotBlank
    @JsonProperty("deal_id")
    private String dealId;

    @NotBlank
    @JsonProperty("filename")
    private String filename;

    @NotBlank
    @JsonProperty("status")
    private String status;

    public ExtractionFailureEvent() {}

    public Long getDocId() { return docId; }
    public void setDocId(Long docId) { this.docId = docId; }

    public String getDealId() { return dealId; }
    public void setDealId(String dealId) { this.dealId = dealId; }

    public String getFilename() { return filename; }
    public void setFilename(String filename) { this.filename = filename; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
