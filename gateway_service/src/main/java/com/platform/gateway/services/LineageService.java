package com.platform.gateway.services;

import com.platform.gateway.dto.LineageResponse;
import com.platform.gateway.dto.MetricSaveRequest;
import com.platform.gateway.entities.AuditLedger;
import com.platform.gateway.entities.DocumentCoordinate;
import com.platform.gateway.entities.ExtractedMetric;
import com.platform.gateway.repositories.AuditLedgerRepository;
import com.platform.gateway.repositories.CoordinateRepository;
import com.platform.gateway.repositories.ExtractedMetricRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import org.springframework.data.domain.Sort;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

@Service
public class LineageService {

    private static final Logger log = LoggerFactory.getLogger(LineageService.class);

    private final ExtractedMetricRepository metricRepo;
    private final CoordinateRepository coordinateRepo;
    private final AuditLedgerRepository auditRepo;

    public LineageService(ExtractedMetricRepository metricRepo,
                          CoordinateRepository coordinateRepo,
                          AuditLedgerRepository auditRepo) {
        this.metricRepo = metricRepo;
        this.coordinateRepo = coordinateRepo;
        this.auditRepo = auditRepo;
    }

    /**
     * Returns lineage for every metric that has a matching coordinate row.
     * Metrics without coordinates are skipped — they lack the bbox needed for highlighting.
     */
    @Transactional(readOnly = true)
    public List<LineageResponse> listAll() {
        List<ExtractedMetric> metrics = metricRepo.findAll(Sort.by(Sort.Direction.DESC, "id"));
        if (metrics.isEmpty()) return List.of();

        Map<Long, DocumentCoordinate> coordByMetricId = coordinateRepo
                .findAllByMetricIdIn(metrics.stream().map(ExtractedMetric::getId).collect(Collectors.toList()))
                .stream()
                .collect(Collectors.toMap(DocumentCoordinate::getMetricId, c -> c));

        return metrics.stream()
                .map(m -> {
                    DocumentCoordinate c = coordByMetricId.get(m.getId());
                    // Coordinates are written by Story 2.x flows. Metrics ingested without
                    // coordinates (Story 1.x classification-only path) still appear in the
                    // list; the highlight box is empty ([0,0,0,0]) but the page still opens.
                    List<Double> bbox = (c != null)
                            ? List.of(c.getXMin(), c.getYMin(), c.getXMax(), c.getYMax())
                            : List.of(0.0, 0.0, 0.0, 0.0);
                    Integer pageNumber = (c != null) ? c.getPageNumber() : m.getPageNumber();
                    return new LineageResponse(
                            m.getId(),
                            m.getMetricName(),
                            m.getRawValue(),
                            m.getSourceDocId(),
                            pageNumber,
                            bbox);
                })
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public LineageResponse getLineage(Long metricId) {
        ExtractedMetric metric = metricRepo.findById(metricId)
                .orElseThrow(() -> new NoSuchElementException("Metric not found: " + metricId));
        DocumentCoordinate coord = coordinateRepo.findByMetricId(metricId)
                .orElseThrow(() -> new NoSuchElementException("No coordinates for metric: " + metricId));

        return new LineageResponse(
                metric.getId(),
                metric.getMetricName(),
                metric.getRawValue(),
                metric.getSourceDocId(),
                coord.getPageNumber(),
                List.of(coord.getXMin(), coord.getYMin(), coord.getXMax(), coord.getYMax())
        );
    }

    /**
     * Saves a metric and its spatial coordinate in a single transaction.
     * If the coordinate save fails, the metric save is also rolled back — no partial audit records.
     */
    @Transactional
    public LineageResponse saveMetricWithCoordinate(MetricSaveRequest req) {
        ExtractedMetric metric = new ExtractedMetric();
        metric.setMetricName(req.getMetricName());
        metric.setRawValue(req.getRawValue());
        metric.setUnit(req.getUnit());
        metric.setSourceDocId(req.getSourceDocId());
        metric.setPageNumber(req.getPageNumber());
        ExtractedMetric saved = metricRepo.save(metric);
        log.info("Saved metric id={} name={}", saved.getId(), saved.getMetricName());

        DocumentCoordinate coord = new DocumentCoordinate();
        coord.setMetricId(saved.getId());
        coord.setPageNumber(req.getPageNumber());
        coord.setXMin(req.getXMin());
        coord.setYMin(req.getYMin());
        coord.setXMax(req.getXMax());
        coord.setYMax(req.getYMax());
        coordinateRepo.save(coord);
        log.info("Saved coordinate for metric id={}", saved.getId());

        return getLineage(saved.getId());
    }

    /**
     * Overwrites a metric's raw_value and writes an immutable audit row capturing the change.
     * Both writes occur in a single transaction — the audit row is never left orphaned.
     */
    @Transactional
    public void overwriteMetric(Long metricId, String newValue, String userId) {
        ExtractedMetric metric = metricRepo.findById(metricId)
                .orElseThrow(() -> new NoSuchElementException("Metric not found: " + metricId));

        String originalValue = metric.getRawValue();
        metric.setRawValue(newValue);
        metricRepo.save(metric);

        AuditLedger entry = new AuditLedger();
        entry.setMetricId(String.valueOf(metricId));
        entry.setOriginalValue(originalValue);
        entry.setNewValue(newValue);
        entry.setModifiedByUserId(userId);
        auditRepo.save(entry);
        log.info("Audit log: metric={} changed by {} from '{}' to '{}'",
                metricId, userId, originalValue, newValue);
    }
}
