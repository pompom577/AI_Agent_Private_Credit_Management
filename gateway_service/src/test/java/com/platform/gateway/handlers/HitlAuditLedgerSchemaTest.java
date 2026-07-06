package com.platform.gateway.handlers;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.jdbc.Sql;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * TC-DB-01: hitl_audit_ledger exists with all required columns.
 * TC-DB-02/03 (append-only): @Disabled — same reason as AuditSchemaTest, H2 cannot
 *   run the PL/pgSQL protect_audit_ledger() trigger from 07-hitl-audit-ledger.sql.
 * Valid-insert + FK-restrict cases below cover what H2 *can* verify: the ledger
 * schema itself and its link back to the parked quarantine row.
 */
@JdbcTest
@Sql("/schema-constraint-test.sql")
class HitlAuditLedgerSchemaTest {

    @Autowired
    JdbcTemplate jdbc;

    private UUID quarantineId;

    @BeforeEach
    void seed() {
        quarantineId = UUID.randomUUID();
        jdbc.update("INSERT INTO quarantine (quarantine_id, endpoint, payload, agent_id, status) " +
                        "VALUES (?,?,?,?,?)",
                quarantineId, "/api/credit/approve", "{\"amount\":50000}", "agent-007", "Pending");
    }

    /**
     * TC-DB-01: the ledger must carry all five business fields so every Approve/Reject
     * decision is fully attributable without relying on application logs.
     */
    @Test
    void tcDb01_hitlAuditLedger_hasAllRequiredColumns() {
        List<String> cols = jdbc.queryForList(
                "SELECT COLUMN_NAME FROM INFORMATION_SCHEMA.COLUMNS " +
                "WHERE TABLE_NAME = 'HITL_AUDIT_LEDGER'",
                String.class);

        assertThat(cols).containsExactlyInAnyOrder(
                "AUDIT_ID", "ACTION_URL", "TIMESTAMP", "DECISION", "OFFICER_ID", "QUARANTINE_ID");
    }

    /**
     * TC-DB-02/03: UPDATE/DELETE on hitl_audit_ledger must be rejected by the
     * protect_audit_ledger() PL/pgSQL trigger defined in 07-hitl-audit-ledger.sql.
     * H2 does not support PL/pgSQL, so this must be verified against real PostgreSQL
     * (e.g. via docker-compose up and a manual integration run).
     */
    @Test
    @Disabled("Requires PostgreSQL — PL/pgSQL trigger cannot execute in H2")
    void tcDb02_03_appendOnly_updateOrDeleteOnLedger_isRejected() {
        // On PostgreSQL: INSERT one row, then attempt UPDATE/DELETE -> expect SQLSTATE P0001
    }

    /**
     * A correctly-formed decision row must persist without error, confirming the
     * column types (UUID officer_id/quarantine_id, decision string) are usable as-is.
     */
    @Test
    void validInsert_recordsApproveDecisionForParkedPayload() {
        UUID officerId = UUID.randomUUID();
        jdbc.update("INSERT INTO hitl_audit_ledger (action_url, decision, officer_id, quarantine_id) " +
                        "VALUES (?,?,?,?)",
                "/api/credit/approve", "Approve", officerId, quarantineId);

        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM hitl_audit_ledger WHERE quarantine_id = ?",
                Integer.class, quarantineId);
        assertThat(count).isEqualTo(1);
    }

    /**
     * FK ON DELETE RESTRICT: a quarantine row that already has an audit decision
     * attached can never be deleted — the audit trail must never be able to dangle.
     */
    @Test
    void deleteParkedPayloadWithAuditEntry_blockedByRestrictConstraint() {
        jdbc.update("INSERT INTO hitl_audit_ledger (action_url, decision, officer_id, quarantine_id) " +
                        "VALUES (?,?,?,?)",
                "/api/credit/approve", "Approve", UUID.randomUUID(), quarantineId);

        assertThatThrownBy(() ->
                jdbc.update("DELETE FROM quarantine WHERE quarantine_id = ?", quarantineId))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
