package com.platform.gateway.controllers;

import com.platform.gateway.dto.ClassificationCompletePayload;
import com.platform.gateway.services.DealOrchestrationService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * POST /webhooks/classification-complete
 *
 * Receives the async callback from the FastAPI classification service once the
 * document processing loop finishes, then delegates deal-state promotion to
 * DealOrchestrationService (Story 1.2, Person 3).
 */
@RestController
@RequestMapping("/webhooks")
public class WebhookController {

    private static final Logger log = LoggerFactory.getLogger(WebhookController.class);

    private final DealOrchestrationService dealOrchestrationService;

    public WebhookController(DealOrchestrationService dealOrchestrationService) {
        this.dealOrchestrationService = dealOrchestrationService;
    }

    @PostMapping("/classification-complete")
    public ResponseEntity<Void> classificationComplete(
            @Valid @RequestBody ClassificationCompletePayload payload) {

        log.info("Received classification-complete callback: deal_id={}, status={}, total_documents={}",
                payload.getDealId(), payload.getStatus(), payload.getTotalDocuments());

        dealOrchestrationService.markClassified(payload.getDealId(), payload.getStatus());

        return ResponseEntity.ok().build();
    }
}
