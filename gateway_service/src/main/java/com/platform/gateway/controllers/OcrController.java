package com.platform.gateway.controllers;

import com.platform.gateway.dto.DocumentRecordResponse;
import com.platform.gateway.dto.LineageResponse;
import com.platform.gateway.dto.ManualMetricEntry;
import com.platform.gateway.entities.DocumentRecord;
import com.platform.gateway.entities.ExtractedMetric;
import com.platform.gateway.repositories.DocumentRecordRepository;
import com.platform.gateway.repositories.ExtractedMetricRepository;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/api/v1/documents")
public class OcrController {

    private final DocumentRecordRepository documentRecordRepository;
    private final ExtractedMetricRepository extractedMetricRepository;

    public OcrController(DocumentRecordRepository documentRecordRepository,
                         ExtractedMetricRepository extractedMetricRepository) {
        this.documentRecordRepository = documentRecordRepository;
        this.extractedMetricRepository = extractedMetricRepository;
    }

    @GetMapping("/{id}")
    public ResponseEntity<DocumentRecordResponse> getDocument(@PathVariable Long id) {
        DocumentRecord doc = documentRecordRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Document not found: " + id));
        return ResponseEntity.ok(new DocumentRecordResponse(
                doc.getId(),
                doc.getFilename(),
                doc.getCategory(),
                doc.getStatus(),
                doc.getPageCount()));
    }

    @GetMapping("/{id}/metrics")
    public ResponseEntity<List<LineageResponse>> getDocumentMetrics(@PathVariable Long id) {
        List<ExtractedMetric> metrics = extractedMetricRepository.findBySourceDocIdOrderByIdAsc(id);
        List<LineageResponse> responses = metrics.stream()
                .map(m -> new LineageResponse(
                        m.getId(),
                        m.getMetricName(),
                        m.getRawValue(),
                        m.getSourceDocId(),
                        m.getPageNumber(),
                        List.of(0.0, 0.0, 0.0, 0.0)))
                .toList();
        return ResponseEntity.ok(responses);
    }

    /**
     * Saves manually entered metrics for a document that failed automatic extraction.
     * Inserts each entry into extracted_metrics and marks the document as Extracted.
     */
    @Transactional
    @PostMapping("/{id}/metrics")
    public ResponseEntity<Void> saveManualMetrics(
            @PathVariable Long id,
            @Valid @RequestBody List<ManualMetricEntry> entries) {
        DocumentRecord doc = documentRecordRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Document not found: " + id));

        for (ManualMetricEntry entry : entries) {
            ExtractedMetric metric = new ExtractedMetric();
            metric.setMetricName(entry.metricName());
            metric.setRawValue(entry.rawValue());
            metric.setSourceDocId(id);
            metric.setPageNumber(1);
            extractedMetricRepository.save(metric);
        }

        doc.setStatus("Extracted");
        documentRecordRepository.save(doc);

        return ResponseEntity.noContent().build();
    }
}
