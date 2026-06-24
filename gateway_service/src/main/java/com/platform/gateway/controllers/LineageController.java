package com.platform.gateway.controllers;

import com.platform.gateway.dto.LineageResponse;
import com.platform.gateway.dto.MetricOverwriteRequest;
import com.platform.gateway.dto.MetricSaveRequest;
import com.platform.gateway.services.LineageService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/metrics")
public class LineageController {

    private final LineageService lineageService;

    public LineageController(LineageService lineageService) {
        this.lineageService = lineageService;
    }

    @GetMapping("/{metricId}/lineage")
    public ResponseEntity<LineageResponse> getLineage(@PathVariable Long metricId) {
        return ResponseEntity.ok(lineageService.getLineage(metricId));
    }

    @PostMapping
    public ResponseEntity<LineageResponse> saveMetric(@Valid @RequestBody MetricSaveRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(lineageService.saveMetricWithCoordinate(req));
    }

    @PutMapping("/{metricId}")
    public ResponseEntity<Void> overwriteMetric(@PathVariable Long metricId,
                                                @Valid @RequestBody MetricOverwriteRequest req) {
        lineageService.overwriteMetric(metricId, req.getNewValue(), req.getUserId());
        return ResponseEntity.noContent().build();
    }
}
