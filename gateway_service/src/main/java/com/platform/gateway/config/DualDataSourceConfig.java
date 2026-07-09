package com.platform.gateway.config;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.scheduling.annotation.EnableScheduling;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

/**
 * Task #1 dual-database wiring — active only when gateway.datasource.backup.url
 * (env BACKUP_DB_URL) is non-empty. Primary = spring.datasource.* (AWS RDS),
 * backup = Neon. All JPA traffic flows through {@link FailoverRoutingDataSource}
 * so record viewing reads RDS while it is available and Neon when it is not;
 * {@link com.platform.gateway.services.BackupSyncService} mirrors primary rows
 * into the backup so both databases store the ingested data.
 *
 * <p>When the property is absent or empty (unit tests, single-DB deployments)
 * none of these beans exist and Spring Boot's normal datasource auto-config
 * applies unchanged.</p>
 */
@Configuration
@EnableScheduling
@ConditionalOnExpression("T(org.springframework.util.StringUtils).hasText('${gateway.datasource.backup.url:}')")
public class DualDataSourceConfig {

    /** Fail fast on a dead database instead of Hikari's 30s default. */
    private static final long CONNECTION_TIMEOUT_MS = 5_000;

    @Bean
    @ConfigurationProperties("spring.datasource")
    public DataSourceProperties primaryDataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean
    @ConfigurationProperties("gateway.datasource.backup")
    public DataSourceProperties backupDataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean
    public HikariDataSource primaryDataSource() {
        return buildPool(primaryDataSourceProperties(), "primary-rds");
    }

    @Bean
    public HikariDataSource backupDataSource() {
        return buildPool(backupDataSourceProperties(), "backup-neon");
    }

    @Bean
    @Primary
    public FailoverRoutingDataSource routingDataSource(
            @Qualifier("primaryDataSource") DataSource primary,
            @Qualifier("backupDataSource") DataSource backup,
            @Value("${gateway.datasource.health-check-interval-ms:5000}") long healthCheckIntervalMs) {
        FailoverRoutingDataSource routing = new FailoverRoutingDataSource(primary, healthCheckIntervalMs);
        Map<Object, Object> targets = new HashMap<>();
        targets.put(FailoverRoutingDataSource.PRIMARY, primary);
        targets.put(FailoverRoutingDataSource.BACKUP, backup);
        routing.setTargetDataSources(targets);
        routing.setDefaultTargetDataSource(primary);
        return routing;
    }

    private static HikariDataSource buildPool(DataSourceProperties props, String poolName) {
        HikariDataSource ds = props.initializeDataSourceBuilder().type(HikariDataSource.class).build();
        ds.setPoolName(poolName);
        ds.setConnectionTimeout(CONNECTION_TIMEOUT_MS);
        return ds;
    }
}
