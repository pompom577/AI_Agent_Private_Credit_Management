package com.platform.gateway.services;

import com.platform.gateway.config.FailoverRoutingDataSource;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Task #1 dual-storage: mirrors every primary-database (AWS RDS) table into
 * the backup (Neon) on a fixed schedule, so data ingested through the ZIP
 * upload flow — by this gateway (deals) and by the FastAPI worker
 * (document_records / extracted_metrics) — is stored in both databases.
 *
 * <p>Copy strategy: full-table upsert by primary key in FK-safe order. The two
 * append-only audit tables carry immutability triggers (schema.sql), so they
 * use ON CONFLICT DO NOTHING; mutable tables use DO UPDATE guarded by
 * IS DISTINCT FROM so unchanged rows are not rewritten. After each cycle the
 * backup's serial sequences are bumped past MAX(id) so writes made on the
 * backup during a failover cannot collide with replicated rows.</p>
 *
 * <p>Limitations (accepted for this deployment): replication is one-way
 * (primary → backup) and deletes are not propagated; rows written on the
 * backup while the primary is down must be reconciled with
 * scripts/migrate_neon_to_rds.sh once the primary returns.</p>
 */
@Service
@ConditionalOnExpression("T(org.springframework.util.StringUtils).hasText('${gateway.datasource.backup.url:}')")
public class BackupSyncService {

    private static final Logger log = LoggerFactory.getLogger(BackupSyncService.class);

    private record TableSpec(String table, String pk, boolean mutable, String serialColumn) {}

    // FK-safe order: parents before children (see schema.sql).
    private static final List<TableSpec> TABLES = List.of(
            new TableSpec("deals", "deal_id", true, null),
            new TableSpec("metric_audit_log", "audit_id", false, "audit_id"),
            new TableSpec("document_records", "id", true, "id"),
            new TableSpec("extracted_metrics", "id", true, "id"),
            new TableSpec("document_coordinates", "id", true, "id"),
            new TableSpec("quarantine", "quarantine_id", true, null),
            new TableSpec("hitl_audit_ledger", "audit_id", false, "audit_id"));

    private final HikariDataSource primary;
    private final HikariDataSource backup;
    private final FailoverRoutingDataSource routing;
    private final boolean primaryIsBackup;

    public BackupSyncService(@Qualifier("primaryDataSource") HikariDataSource primary,
                             @Qualifier("backupDataSource") HikariDataSource backup,
                             FailoverRoutingDataSource routing) {
        this.primary = primary;
        this.backup = backup;
        this.routing = routing;
        // Until RDS_ENDPOINT is set, docker-compose points both at Neon; syncing
        // a database onto itself is a pointless round-trip, so skip cycles.
        this.primaryIsBackup = Objects.equals(primary.getJdbcUrl(), backup.getJdbcUrl());
        if (primaryIsBackup) {
            log.warn("Primary and backup datasource URLs are identical — backup sync disabled "
                    + "(set RDS_ENDPOINT to enable RDS-primary/Neon-backup replication)");
        }
    }

    @Scheduled(fixedDelayString = "${gateway.datasource.sync-interval-ms:15000}",
               initialDelayString = "${gateway.datasource.sync-interval-ms:15000}")
    public void replicateToBackup() {
        if (primaryIsBackup) {
            return;
        }
        if (!routing.isPrimaryUp()) {
            log.debug("Skipping backup sync — primary is down, backup is serving traffic");
            return;
        }
        int totalChanged = 0;
        try (Connection src = primary.getConnection(); Connection dst = backup.getConnection()) {
            dst.setAutoCommit(false);
            for (TableSpec spec : TABLES) {
                if (backupAheadOfPrimary(src, dst, spec)) {
                    continue;
                }
                totalChanged += copyTable(src, dst, spec);
            }
            bumpSequences(dst);
            dst.commit();
        } catch (SQLException e) {
            log.warn("Backup sync cycle failed: {}", e.getMessage());
            return;
        }
        if (totalChanged > 0) {
            log.info("Backup sync: {} row(s) replicated to backup database", totalChanged);
        }
    }

    /**
     * Serial-PK guard: if the backup's MAX(id) exceeds the primary's, the backup
     * holds rows the primary does not know about — either the one-time
     * migrate_neon_to_rds.sh step was skipped (fresh primary vs populated backup)
     * or rows were written to the backup during a failover. Upserting would
     * overwrite those rows with unrelated data that happens to share ids, so the
     * table is skipped until the primary is made a superset again (re-run the
     * migration script). UUID-keyed tables cannot collide across datasets and
     * are always safe to sync.
     */
    private boolean backupAheadOfPrimary(Connection src, Connection dst, TableSpec spec) throws SQLException {
        if (spec.serialColumn() == null) {
            return false;
        }
        long primaryMax = maxSerial(src, spec);
        long backupMax = maxSerial(dst, spec);
        if (backupMax > primaryMax) {
            log.warn("Backup table {} has rows the primary lacks (backup max {} > primary max {}) — "
                            + "skipping its sync; run scripts/migrate_neon_to_rds.sh to make the primary "
                            + "a superset, then sync resumes automatically",
                    spec.table(), backupMax, primaryMax);
            return true;
        }
        return false;
    }

    private static long maxSerial(Connection conn, TableSpec spec) throws SQLException {
        String sql = "SELECT COALESCE(MAX(\"" + spec.serialColumn() + "\"), 0) FROM " + spec.table();
        try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            rs.next();
            return rs.getLong(1);
        }
    }

    private int copyTable(Connection src, Connection dst, TableSpec spec) throws SQLException {
        try (Statement st = src.createStatement();
             ResultSet rs = st.executeQuery("SELECT * FROM " + spec.table())) {
            ResultSetMetaData md = rs.getMetaData();
            int cols = md.getColumnCount();
            List<String> names = new ArrayList<>(cols);
            for (int i = 1; i <= cols; i++) {
                names.add(md.getColumnName(i));
            }
            int changed = 0;
            try (PreparedStatement ps = dst.prepareStatement(buildUpsert(spec, names))) {
                int batched = 0;
                while (rs.next()) {
                    for (int i = 1; i <= cols; i++) {
                        ps.setObject(i, rs.getObject(i));
                    }
                    ps.addBatch();
                    if (++batched % 500 == 0) {
                        changed += sum(ps.executeBatch());
                    }
                }
                changed += sum(ps.executeBatch());
            }
            return changed;
        }
    }

    private static String buildUpsert(TableSpec spec, List<String> columns) {
        String colList = columns.stream().map(c -> "\"" + c + "\"").collect(Collectors.joining(", "));
        String placeholders = String.join(", ", Collections.nCopies(columns.size(), "?"));
        String base = "INSERT INTO " + spec.table() + " AS t (" + colList + ") VALUES (" + placeholders + ")"
                + " ON CONFLICT (\"" + spec.pk() + "\")";
        if (!spec.mutable()) {
            return base + " DO NOTHING";
        }
        List<String> nonPk = columns.stream().filter(c -> !c.equals(spec.pk())).toList();
        String sets = nonPk.stream().map(c -> "\"" + c + "\" = EXCLUDED.\"" + c + "\"")
                .collect(Collectors.joining(", "));
        String current = nonPk.stream().map(c -> "t.\"" + c + "\"").collect(Collectors.joining(", "));
        String incoming = nonPk.stream().map(c -> "EXCLUDED.\"" + c + "\"").collect(Collectors.joining(", "));
        return base + " DO UPDATE SET " + sets
                + " WHERE (" + current + ") IS DISTINCT FROM (" + incoming + ")";
    }

    private void bumpSequences(Connection dst) throws SQLException {
        for (TableSpec spec : TABLES) {
            if (spec.serialColumn() == null) {
                continue;
            }
            String sql = "SELECT setval(pg_get_serial_sequence('" + spec.table() + "', '" + spec.serialColumn()
                    + "'), (SELECT COALESCE(MAX(\"" + spec.serialColumn() + "\"), 0) + 1 FROM "
                    + spec.table() + "), false)";
            try (Statement st = dst.createStatement()) {
                st.execute(sql);
            }
        }
    }

    private static int sum(int[] counts) {
        int total = 0;
        for (int c : counts) {
            // Statement.SUCCESS_NO_INFO (-2) still means the row was applied.
            total += (c > 0 || c == Statement.SUCCESS_NO_INFO) ? 1 : 0;
        }
        return total;
    }
}
