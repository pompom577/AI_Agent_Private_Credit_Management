package com.platform.gateway.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "hitl_audit_ledger")
public class HitlAuditLedger {

    public static final String DECISION_APPROVE = "Approve";
    public static final String DECISION_REJECT = "Reject";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "audit_id")
    private Long auditId;

    @Column(name = "action_url", nullable = false, length = 500)
    private String actionUrl;

    @Column(name = "decision", nullable = false, length = 20)
    private String decision;

    @Column(name = "officer_id", nullable = false, columnDefinition = "uuid")
    private UUID officerId;

    @Column(name = "quarantine_id", nullable = false, columnDefinition = "uuid")
    private UUID quarantineId;

    @Column(name = "timestamp", insertable = false, updatable = false)
    private OffsetDateTime timestamp;

    public HitlAuditLedger() {}

    public Long getAuditId() { return auditId; }
    public String getActionUrl() { return actionUrl; }
    public void setActionUrl(String actionUrl) { this.actionUrl = actionUrl; }
    public String getDecision() { return decision; }
    public void setDecision(String decision) { this.decision = decision; }
    public UUID getOfficerId() { return officerId; }
    public void setOfficerId(UUID officerId) { this.officerId = officerId; }
    public UUID getQuarantineId() { return quarantineId; }
    public void setQuarantineId(UUID quarantineId) { this.quarantineId = quarantineId; }
    public OffsetDateTime getTimestamp() { return timestamp; }
}
