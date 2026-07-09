package com.platform.gateway.controllers;

import com.platform.gateway.entities.HitlAuditLedger;
import com.platform.gateway.entities.QuarantinedPayload;
import com.platform.gateway.repositories.QuarantineRepository;
import com.platform.gateway.services.AuditLoggingService;
import com.platform.gateway.services.QuarantineStreamService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/quarantine")
public class QuarantineController {

    private final QuarantineRepository quarantineRepository;
    private final WebClient destinationWebClient;
    private final QuarantineStreamService streamService;
    private final AuditLoggingService auditLoggingService;

    public QuarantineController(QuarantineRepository quarantineRepository,
                                WebClient destinationWebClient,
                                QuarantineStreamService streamService,
                                AuditLoggingService auditLoggingService) {
        this.quarantineRepository = quarantineRepository;
        this.destinationWebClient = destinationWebClient;
        this.streamService = streamService;
        this.auditLoggingService = auditLoggingService;
    }

    @GetMapping
    public List<QuarantinedPayload> getQuarantineQueue() {
        return quarantineRepository.findByStatusInOrderByCreatedAtDesc(List.of(
                QuarantinedPayload.STATUS_PENDING,
                QuarantinedPayload.STATUS_DELIVERY_TIMEOUT_WARNING,
                QuarantinedPayload.STATUS_EXECUTION_FAILED
        ));
    }

    /** Real-time queue feed for the compliance dashboard — TC-UI-01. */
    @GetMapping("/stream")
    public SseEmitter stream() {
        return streamService.subscribe();
    }

    /**
     * @Transactional (3.3a): the quarantine status update and the audit ledger
     * INSERT commit or roll back together. If the ledger write fails, the payload
     * must not be released/discarded — it stays Pending and the officer sees a 500.
     */
    @Transactional
    @PostMapping("/{id}/reject")
    public ResponseEntity<QuarantinedPayload> reject(@PathVariable UUID id,
                                                      @RequestHeader("X-User-Id") UUID officerId) {
        QuarantinedPayload payload = quarantineRepository.findById(id).orElseThrow();
        payload.setStatus(QuarantinedPayload.STATUS_DISCARDED);
        QuarantinedPayload saved = quarantineRepository.save(payload);

        auditLoggingService.recordDecision(saved.getEndpoint(), HitlAuditLedger.DECISION_REJECT,
                officerId, saved.getQuarantineId());

        streamService.broadcast(saved);
        return ResponseEntity.ok(saved);
    }

    @Transactional
    @PostMapping("/{id}/approve")
    public ResponseEntity<QuarantinedPayload> approve(@PathVariable UUID id,
                                                       @RequestHeader("X-User-Id") UUID officerId) {
        QuarantinedPayload payload = quarantineRepository.findById(id).orElseThrow();

        try {
            destinationWebClient.post()
                    .uri(payload.getEndpoint())
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(payload.getPayload())
                    .retrieve()
                    .toBodilessEntity()
                    .block();

            payload.setStatus(QuarantinedPayload.STATUS_APPROVED);
        } catch (Exception e) {
            payload.setStatus(QuarantinedPayload.STATUS_EXECUTION_FAILED);
        }

        QuarantinedPayload saved = quarantineRepository.save(payload);

        // The officer's decision was "Approve" regardless of whether the downstream
        // forward technically succeeded — that outcome is tracked by payload status.
        auditLoggingService.recordDecision(saved.getEndpoint(), HitlAuditLedger.DECISION_APPROVE,
                officerId, saved.getQuarantineId());

        streamService.broadcast(saved);
        return ResponseEntity.ok(saved);
    }
}