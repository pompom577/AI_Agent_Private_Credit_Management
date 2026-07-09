package com.platform.gateway.services;

import com.platform.gateway.entities.HitlAuditLedger;
import com.platform.gateway.repositories.HitlAuditLedgerRepo;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Composes and persists a {@link HitlAuditLedger} row for every Approve/Reject
 * decision — Sub-Story 3.3a. Callers run this inside their own @Transactional
 * boundary so a failed INSERT here rolls back the payload release/discard too
 * (see QuarantineController).
 */
@Service
public class AuditLoggingService {

    private final HitlAuditLedgerRepo auditLedgerRepo;
    private final Counter writeSuccessCounter;
    private final Counter writeFailureCounter;

    public AuditLoggingService(HitlAuditLedgerRepo auditLedgerRepo, MeterRegistry meterRegistry) {
        this.auditLedgerRepo = auditLedgerRepo;
        this.writeSuccessCounter = Counter.builder("hitl_audit_ledger_writes_total")
                .description("Successful INSERTs into hitl_audit_ledger")
                .register(meterRegistry);
        this.writeFailureCounter = Counter.builder("hitl_audit_ledger_write_failures_total")
                .description("Failed INSERTs into hitl_audit_ledger — TC-OPS-01 pages on any increase")
                .register(meterRegistry);
    }

    public void recordDecision(String actionUrl, String decision, UUID officerId, UUID quarantineId) {
        HitlAuditLedger entry = new HitlAuditLedger();
        entry.setActionUrl(actionUrl);
        entry.setDecision(decision);
        entry.setOfficerId(officerId);
        entry.setQuarantineId(quarantineId);

        try {
            auditLedgerRepo.save(entry);
            writeSuccessCounter.increment();
        } catch (RuntimeException e) {
            writeFailureCounter.increment();
            throw e; // propagate — the caller's @Transactional boundary must roll back
        }
    }
}
