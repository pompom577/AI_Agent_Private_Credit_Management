package com.platform.gateway.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;

/**
 * JPA entity mapping to the existing {@code deals} table.
 * Used by DealOrchestrationService to update the overarching deal status
 * when the classification-complete webhook is received (Story 1.2, Person 3).
 */
@Entity
@Table(name = "deals")
public class DealState {

    @Id
    @Column(name = "deal_id", nullable = false, length = 255)
    private String dealId;

    @Column(name = "uploaded_by_user_id", nullable = false, length = 255)
    private String uploadedByUserId;

    @Column(name = "bucket_url", nullable = false, columnDefinition = "TEXT")
    private String bucketUrl;

    @Column(name = "status", length = 50)
    private String status;

    @Column(name = "created_at", insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    public DealState() {}

    public String getDealId() { return dealId; }
    public void setDealId(String dealId) { this.dealId = dealId; }

    public String getUploadedByUserId() { return uploadedByUserId; }
    public void setUploadedByUserId(String uploadedByUserId) { this.uploadedByUserId = uploadedByUserId; }

    public String getBucketUrl() { return bucketUrl; }
    public void setBucketUrl(String bucketUrl) { this.bucketUrl = bucketUrl; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
}
