package com.platform.gateway.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * Task #1 dual-DB: routes all JDBC traffic (JPA reads and writes) to the
 * primary datasource (AWS RDS) while it is healthy, failing over to the
 * backup (Neon) when it is not — so record viewing keeps working through a
 * primary outage.
 *
 * <p>The primary is probed at most once per {@code checkIntervalMs}; between
 * probes the last known state is reused, so an outage is detected within one
 * interval (default 5s) plus the pool's 5s connection timeout.</p>
 */
public class FailoverRoutingDataSource extends AbstractRoutingDataSource {

    private static final Logger log = LoggerFactory.getLogger(FailoverRoutingDataSource.class);

    static final String PRIMARY = "primary";
    static final String BACKUP = "backup";

    private final DataSource primary;
    private final long checkIntervalMs;

    private volatile boolean primaryUp = true;
    private volatile long lastProbeAtMs = 0L;

    public FailoverRoutingDataSource(DataSource primary, long checkIntervalMs) {
        this.primary = primary;
        this.checkIntervalMs = checkIntervalMs;
    }

    @Override
    protected Object determineCurrentLookupKey() {
        long now = System.currentTimeMillis();
        if (now - lastProbeAtMs >= checkIntervalMs) {
            lastProbeAtMs = now;
            boolean wasUp = primaryUp;
            primaryUp = probePrimary();
            if (wasUp && !primaryUp) {
                log.warn("Primary database unreachable — failing over reads/writes to the backup (Neon)");
            } else if (!wasUp && primaryUp) {
                log.info("Primary database recovered — routing traffic back to primary (RDS)");
            }
        }
        return primaryUp ? PRIMARY : BACKUP;
    }

    /** Used by BackupSyncService to skip replication cycles while the primary is down. */
    public boolean isPrimaryUp() {
        determineCurrentLookupKey();
        return primaryUp;
    }

    private boolean probePrimary() {
        try (Connection conn = primary.getConnection()) {
            return conn.isValid(2);
        } catch (SQLException e) {
            log.debug("Primary health probe failed: {}", e.getMessage());
            return false;
        }
    }
}
