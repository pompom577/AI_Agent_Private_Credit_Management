package com.platform.gateway.handlers;

import com.platform.gateway.dto.ExtractionFailureEvent;
import com.platform.gateway.entities.DealState;
import com.platform.gateway.messaging.NotificationBridge;
import com.platform.gateway.repositories.DealStateRepository;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * POST /webhooks/extraction-failed  — receives extraction failure from FastAPI, routes to analyst via SSE.
 * GET  /sse/extraction-updates      — analyst subscribes to their personal SSE stream.
 * (Story 1.3, Person 3 — TC-GW-03/04)
 */
@RestController
public class InternalEventHandler {

    private static final Logger log = LoggerFactory.getLogger(InternalEventHandler.class);

    private final NotificationBridge notificationBridge;
    private final DealStateRepository dealStateRepository;
    private final Long sseTimeoutMs;

    public InternalEventHandler(NotificationBridge notificationBridge,
                                DealStateRepository dealStateRepository,
                                Long sseTimeoutMs) {
        this.notificationBridge = notificationBridge;
        this.dealStateRepository = dealStateRepository;
        this.sseTimeoutMs = sseTimeoutMs;
    }

    @PostMapping("/webhooks/extraction-failed")
    public ResponseEntity<Void> extractionFailed(@Valid @RequestBody ExtractionFailureEvent event) {
        log.info("Received extraction-failed callback: doc_id={}, deal_id={}, status={}",
                event.getDocId(), event.getDealId(), event.getStatus());

        DealState deal = dealStateRepository.findById(event.getDealId())
                .orElseThrow(() -> new IllegalArgumentException("Unknown deal_id: " + event.getDealId()));

        notificationBridge.push(deal.getUploadedByUserId(), event);

        return ResponseEntity.ok().build();
    }

    @GetMapping("/sse/extraction-updates")
    public SseEmitter subscribe(@RequestParam String userId) {
        SseEmitter emitter = new SseEmitter(sseTimeoutMs);
        notificationBridge.register(userId, emitter);
        log.info("SSE subscription opened: userId={}", userId);
        return emitter;
    }
}
