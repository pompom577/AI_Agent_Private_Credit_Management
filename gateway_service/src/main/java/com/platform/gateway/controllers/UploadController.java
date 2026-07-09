package com.platform.gateway.controllers;

import com.platform.gateway.clients.FastApiClient;
import com.platform.gateway.dto.ClassifyRequest;
import com.platform.gateway.dto.UploadResponse;
import com.platform.gateway.services.DealOrchestrationService;
import com.platform.gateway.services.JwtService;
import com.platform.gateway.services.StorageService;
import com.platform.gateway.services.ZipInspectionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

/**
 * POST /uploads — multipart ZIP ingestion endpoint.
 *
 * Pipeline: validate magic bytes (1.1a) -> inspect entries / reject encrypted (1.1b)
 * -> persist to S3 (1.1a) -> mint short-lived JWT + hand off to FastAPI /classify (1.1c)
 * -> return 201 {bucket_url, deal_id}.
 */
@RestController
@RequestMapping("/uploads")
public class UploadController {

    private static final Logger log = LoggerFactory.getLogger(UploadController.class);

    private final ZipInspectionService zipInspectionService;
    private final StorageService storageService;
    private final JwtService jwtService;
    private final FastApiClient fastApiClient;
    private final DealOrchestrationService dealOrchestrationService;

    public UploadController(ZipInspectionService zipInspectionService,
                            StorageService storageService,
                            JwtService jwtService,
                            FastApiClient fastApiClient,
                            DealOrchestrationService dealOrchestrationService) {
        this.zipInspectionService = zipInspectionService;
        this.storageService = storageService;
        this.jwtService = jwtService;
        this.fastApiClient = fastApiClient;
        this.dealOrchestrationService = dealOrchestrationService;
    }

    @PostMapping
    public ResponseEntity<UploadResponse> upload(
            @RequestParam("file") MultipartFile file,
            @RequestHeader(value = "X-User-Id", required = false) String uploadedByUserId) {

        log.info("Received upload: name={}, size={}, contentType={}, user={}",
                file.getOriginalFilename(), file.getSize(), file.getContentType(), uploadedByUserId);

        // 1.1a — magic-byte / format check (throws InvalidArchiveFormatException -> 415).
        zipInspectionService.validateMagicBytes(file);

        // 1.1b — deep entry inspection (UnsupportedArchiveEntryException -> 415)
        //        then encryption check (EncryptedArchiveException -> 422).
        // Diagram order: unsupported entries are reported before encryption.
        zipInspectionService.inspectEntries(file);
        zipInspectionService.assertNotEncrypted(file);

        String dealId = UUID.randomUUID().toString();
        String bucketUrl = storageService.put(dealId, file);

        // Default to "anonymous" so the downstream service (which requires a non-empty
        // uploaded_by_user_id) accepts unauthenticated uploads consistently with JWT sub.
        String effectiveUserId = (uploadedByUserId == null || uploadedByUserId.isBlank())
                ? "anonymous"
                : uploadedByUserId;

        // Persist the parent deal row BEFORE the classification handoff. The FastAPI
        // worker writes document_records rows that carry an FK to deals(deal_id); if
        // this row is missing every insert fails and nothing gets classified.
        dealOrchestrationService.recordIngestedDeal(dealId, effectiveUserId, bucketUrl);

        // 1.1c — mint short-lived internal JWT and hand off to FastAPI /classify.
        // ClassificationHandoffException -> propagated as upstream status (401/422/502).
        String jwt = jwtService.mintInternalToken(effectiveUserId, dealId);
        fastApiClient.classify(jwt, new ClassifyRequest(bucketUrl, dealId, effectiveUserId));

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new UploadResponse(bucketUrl, dealId));
    }
}
