package com.platform.gateway.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;

@Entity
@Table(name = "metric_audit_log")
public class AuditLedger {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "audit_id")
    private Long auditId;

    @Column(name = "metric_id", nullable = false, length = 255)
    private String metricId;

    @Column(name = "original_value", nullable = false, length = 255)
    private String originalValue;

    @Column(name = "new_value", nullable = false, length = 255)
    private String newValue;

    @Column(name = "modified_by_user_id", nullable = false, length = 255)
    private String modifiedByUserId;

    @Column(name = "timestamp", insertable = false, updatable = false)
    private OffsetDateTime timestamp;

    public AuditLedger() {}

    public Long getAuditId() { return auditId; }
    public String getMetricId() { return metricId; }
    public void setMetricId(String metricId) { this.metricId = metricId; }
    public String getOriginalValue() { return originalValue; }
    public void setOriginalValue(String originalValue) { this.originalValue = originalValue; }
    public String getNewValue() { return newValue; }
    public void setNewValue(String newValue) { this.newValue = newValue; }
    public String getModifiedByUserId() { return modifiedByUserId; }
    public void setModifiedByUserId(String modifiedByUserId) { this.modifiedByUserId = modifiedByUserId; }
    public OffsetDateTime getTimestamp() { return timestamp; }
}
