package com.platform.gateway.services;

import com.platform.gateway.dto.LineageResponse;
import com.platform.gateway.dto.MetricSaveRequest;
import com.platform.gateway.entities.AuditLedger;
import com.platform.gateway.repositories.AuditLedgerRepository;
import com.platform.gateway.repositories.CoordinateRepository;
import com.platform.gateway.repositories.ExtractedMetricRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;

/**
 * TC-BE-02: Transactional atomicity — coordinate failure rolls back the metric save.
 * TC-BE-03: Immutable overwrite logging — overwriteMetric writes an audit row.
 *
 * Uses @Import(LineageService.class) so the service is a Spring proxy and
 * @Transactional is properly enforced (not bypassed as it would be with `new`).
 */
@DataJpaTest(properties = {
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=none",
        "spring.sql.init.schema-locations=classpath:lineage-service-test.sql"
})
@Import(LineageService.class)
class LineageServiceTest {

    @Autowired
    LineageService lineageService;

    @Autowired
    ExtractedMetricRepository metricRepo;

    @SpyBean
    CoordinateRepository coordinateRepo;

    @Autowired
    AuditLedgerRepository auditRepo;

    @BeforeEach
    void resetSpy() {
        Mockito.reset(coordinateRepo);
    }

    private MetricSaveRequest buildRequest() {
        MetricSaveRequest req = new MetricSaveRequest();
        req.setMetricName("Revenue");
        req.setRawValue("$1.5M");
        req.setUnit("USD");
        req.setSourceDocId(1L);
        req.setPageNumber(3);
        req.setXMin(10.0);
        req.setYMin(20.0);
        req.setXMax(100.0);
        req.setYMax(40.0);
        return req;
    }

    /**
     * TC-BE-02: When the coordinate save fails, the metric save must also roll back.
     * A partial metric row with no coordinate is un-auditable — lineage queries would 404.
     *
     * The service must be Spring-proxied for @Transactional to apply; manual `new`
     * construction bypasses the proxy and makes the metric commit independently.
     */
    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void tcBe02_coordinateFailureRollsBackMetricSave() {
        doThrow(new RuntimeException("Simulated DB failure on coordinate save"))
                .when(coordinateRepo).save(any());

        assertThatThrownBy(() -> lineageService.saveMetricWithCoordinate(buildRequest()))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Simulated DB failure");

        assertThat(metricRepo.count()).isZero();
    }

    /**
     * TC-BE-03: Overwriting a metric must write an audit row capturing the original value,
     * new value, and user — so every manual change is traceable without application logs.
     */
    @Test
    void tcBe03_overwriteMetric_writesAuditRow() {
        LineageResponse saved = lineageService.saveMetricWithCoordinate(buildRequest());

        lineageService.overwriteMetric(saved.metricId(), "$2.0M", "analyst-1");

        List<AuditLedger> entries = auditRepo.findAll();
        assertThat(entries).hasSize(1);
        AuditLedger entry = entries.get(0);
        assertThat(entry.getMetricId()).isEqualTo(String.valueOf(saved.metricId()));
        assertThat(entry.getOriginalValue()).isEqualTo("$1.5M");
        assertThat(entry.getNewValue()).isEqualTo("$2.0M");
        assertThat(entry.getModifiedByUserId()).isEqualTo("analyst-1");
    }
}
