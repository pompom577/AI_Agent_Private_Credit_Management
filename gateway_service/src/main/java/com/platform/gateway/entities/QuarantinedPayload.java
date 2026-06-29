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
@Table(name = "quarantine")
public class QuarantinedPayload {

    public static final String STATUS_PENDING                  = "Pending";
    public static final String STATUS_DELIVERY_TIMEOUT_WARNING = "Delivery_Timeout_Warning";
    public static final String STATUS_APPROVED                 = "Approved";
    public static final String STATUS_DISCARDED                = "Discarded";
    public static final String STATUS_EXECUTION_FAILED         = "Execution_Failed";

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "quarantine_id", columnDefinition = "uuid")
    private UUID quarantineId;

    @Column(name = "endpoint", nullable = false, length = 500)
    private String endpoint;

    // Stored as JSONB in PostgreSQL — raw request body from the AI agent.
    @Column(name = "payload", columnDefinition = "jsonb", nullable = false)
    private String payload;

    @Column(name = "agent_id", length = 255)
    private String agentId;

    @Column(name = "status", nullable = false, length = 50)
    private String status;

    @Column(name = "created_at", insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    public QuarantinedPayload() {}

    public UUID getQuarantineId()             { return quarantineId; }
    public String getEndpoint()               { return endpoint; }
    public void setEndpoint(String endpoint)  { this.endpoint = endpoint; }
    public String getPayload()                { return payload; }
    public void setPayload(String payload)    { this.payload = payload; }
    public String getAgentId()                { return agentId; }
    public void setAgentId(String agentId)    { this.agentId = agentId; }
    public String getStatus()                 { return status; }
    public void setStatus(String status)      { this.status = status; }
    public OffsetDateTime getCreatedAt()      { return createdAt; }
    public OffsetDateTime getUpdatedAt()      { return updatedAt; }
}
