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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * TC-DB-01: metric_audit_log exists with all required columns.
 * TC-DB-02: Append-only trigger blocks UPDATE/DELETE — @Disabled because H2 cannot run PL/pgSQL.
 * TC-DB-03: Valid document_coordinates INSERT succeeds.
 * TC-DB-04: ON DELETE RESTRICT blocks deletion of a metric that has a linked coordinate row.
 */
@JdbcTest
@Sql("/schema-constraint-test.sql")
class AuditSchemaTest {

    @Autowired
    JdbcTemplate jdbc;

    private long docId;

    @BeforeEach
    void seed() {
        jdbc.update("INSERT INTO deals (deal_id, uploaded_by_user_id, bucket_url) VALUES (?,?,?)",
                "deal-audit", "user-1", "s3://bucket/deal-audit");
        jdbc.update("INSERT INTO document_records (deal_id, filename, status) VALUES (?,?,?)",
                "deal-audit", "financials.pdf", "PENDING");
        docId = jdbc.queryForObject(
                "SELECT id FROM document_records WHERE filename = 'financials.pdf'", Long.class);
    }

    /**
     * TC-DB-01: The audit ledger must carry all five non-nullable business fields so that
     * every overwrite is fully attributable without relying on application logs.
     */
    @Test
    void tcDb01_auditLogTable_hasAllRequiredColumns() {
        List<String> cols = jdbc.queryForList(
                "SELECT COLUMN_NAME FROM INFORMATION_SCHEMA.COLUMNS " +
                "WHERE TABLE_NAME = 'METRIC_AUDIT_LOG'",
                String.class);

        assertThat(cols).containsExactlyInAnyOrder(
                "AUDIT_ID", "METRIC_ID", "ORIGINAL_VALUE", "NEW_VALUE",
                "MODIFIED_BY_USER_ID", "TIMESTAMP");
    }

    /**
     * TC-DB-02: UPDATE or DELETE on metric_audit_log must be rejected by the
     * protect_audit_ledger() PL/pgSQL trigger defined in 04-audit-ledger-schema.sql.
     * H2 does not support PL/pgSQL, so this case must be verified against the real
     * PostgreSQL instance (e.g. via docker-compose up and a manual integration run).
     */
    @Test
    @Disabled("Requires PostgreSQL — PL/pgSQL trigger cannot execute in H2")
    void tcDb02_appendOnly_updateOnAuditLog_isRejected() {
        // On PostgreSQL: INSERT one row, then attempt UPDATE → expect SQLSTATE P0001
    }

    /**
     * TC-DB-03: A correctly-formed coordinate row must persist without error,
     * confirming the spatial geometry columns are properly initialized.
     */
    @Test
    void tcDb03_coordinateSchema_validInsert_succeeds() {
        long metricId = insertMetric("Total Revenue", "$12.5M");

        jdbc.update(
                "INSERT INTO document_coordinates " +
                "(metric_id, page_number, x_min, y_min, x_max, y_max) VALUES (?,?,?,?,?,?)",
                metricId, 1, 10.5, 20.0, 80.0, 30.0);

        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM document_coordinates WHERE metric_id = ?",
                Integer.class, metricId);
        assertThat(count).isEqualTo(1);
    }

    /**
     * TC-DB-04: ON DELETE RESTRICT on document_coordinates.metric_id must prevent
     * deletion of the parent extracted_metric, ensuring the audit chain is never broken.
     */
    @Test
    void tcDb04_deleteParentMetric_blockedByRestrictConstraint() {
        long metricId = insertMetric("Net EBITDA", "$4.7M");
        jdbc.update(
                "INSERT INTO document_coordinates " +
                "(metric_id, page_number, x_min, y_min, x_max, y_max) VALUES (?,?,?,?,?,?)",
                metricId, 2, 55.0, 72.0, 85.0, 76.0);

        assertThatThrownBy(() ->
                jdbc.update("DELETE FROM extracted_metrics WHERE id = ?", metricId))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    private long insertMetric(String name, String value) {
        jdbc.update(
                "INSERT INTO extracted_metrics (metric_name, raw_value, source_doc_id) VALUES (?,?,?)",
                name, value, docId);
        return jdbc.queryForObject(
                "SELECT id FROM extracted_metrics WHERE metric_name = ?", Long.class, name);
    }
}
