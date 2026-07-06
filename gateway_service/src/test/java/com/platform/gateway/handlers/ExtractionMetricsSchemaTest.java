package com.platform.gateway.handlers;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.jdbc.Sql;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * TC-GW-01: INSERT missing metric_name/raw_value → NOT NULL violation.
 * TC-GW-02: INSERT with non-existent source_doc_id → FK violation.
 *
 * Uses H2 in-memory database (auto-configured by @JdbcTest) with a
 * minimal schema that mirrors the PostgreSQL NOT NULL + FK constraints
 * defined in 03-extracted-metrics-schema.sql.
 */
@JdbcTest
@Sql("/schema-constraint-test.sql")
class ExtractionMetricsSchemaTest {

    @Autowired
    JdbcTemplate jdbc;

    private long validDocId;

    @BeforeEach
    void seedDealAndDocument() {
        jdbc.update(
            "INSERT INTO deals (deal_id, uploaded_by_user_id, bucket_url) VALUES (?, ?, ?)",
            "deal-1", "analyst-1", "s3://bucket/deal-1"
        );
        jdbc.update(
            "INSERT INTO document_records (deal_id, filename, status) VALUES (?, ?, ?)",
            "deal-1", "InvestCo_Report.pdf", "PENDING"
        );
        validDocId = jdbc.queryForObject(
            "SELECT id FROM document_records WHERE filename = 'InvestCo_Report.pdf'",
            Long.class
        );
    }

    /**
     * TC-GW-01: metric_name NOT NULL constraint prevents bad data from entering the ledger.
     * Attempting to omit metric_name must be rejected by the database, not silently stored as NULL.
     */
    @Test
    void tcGw01_insertMissingMetricName_throwsNotNullViolation() {
        assertThatThrownBy(() ->
            jdbc.update(
                "INSERT INTO extracted_metrics (raw_value, source_doc_id) VALUES (?, ?)",
                "$1.5M", validDocId
            )
        ).isInstanceOf(DataIntegrityViolationException.class);
    }

    /**
     * TC-GW-01 (raw_value variant): raw_value NOT NULL prevents partial metric rows.
     */
    @Test
    void tcGw01_insertMissingRawValue_throwsNotNullViolation() {
        assertThatThrownBy(() ->
            jdbc.update(
                "INSERT INTO extracted_metrics (metric_name, source_doc_id) VALUES (?, ?)",
                "Revenue", validDocId
            )
        ).isInstanceOf(DataIntegrityViolationException.class);
    }

    /**
     * TC-GW-02: FK constraint on source_doc_id prevents orphaned metrics
     * when the parent document_record doesn't exist.
     */
    @Test
    void tcGw02_insertOrphanSourceDocId_throwsForeignKeyViolation() {
        long nonExistentDocId = 99999L;

        assertThatThrownBy(() ->
            jdbc.update(
                "INSERT INTO extracted_metrics (metric_name, raw_value, source_doc_id) VALUES (?, ?, ?)",
                "Revenue", "$1.5M", nonExistentDocId
            )
        ).isInstanceOf(DataIntegrityViolationException.class);
    }
}
