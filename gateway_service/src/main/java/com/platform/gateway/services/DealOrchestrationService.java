package com.platform.gateway.services;

import com.platform.gateway.entities.DealState;
import com.platform.gateway.repositories.DealStateRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

/**
 * Updates the overarching deal status when the FastAPI classification-complete
 * callback is received (Story 1.2, Person 3).
 */
@Service
public class DealOrchestrationService {

    private static final Logger log = LoggerFactory.getLogger(DealOrchestrationService.class);

    private final DealStateRepository dealStateRepository;

    public DealOrchestrationService(DealStateRepository dealStateRepository) {
        this.dealStateRepository = dealStateRepository;
    }

    /**
     * Persists the parent {@code deals} row at ingestion time.
     *
     * <p>This MUST run before the FastAPI classification handoff: every
     * {@code document_records} row the worker writes carries a foreign key to
     * {@code deals(deal_id)}, so without this row the worker's inserts fail with
     * an FK violation and nothing is ever classified.</p>
     *
     * @param dealId            the freshly generated deal id
     * @param uploadedByUserId  the uploading user (or "anonymous")
     * @param bucketUrl         the s3:// location the archive was stored at
     */
    @Transactional
    public void recordIngestedDeal(String dealId, String uploadedByUserId, String bucketUrl) {
        DealState deal = new DealState();
        deal.setDealId(dealId);
        deal.setUploadedByUserId(uploadedByUserId);
        deal.setBucketUrl(bucketUrl);
        deal.setStatus("INGESTED");
        dealStateRepository.save(deal);
        log.info("Recorded ingested deal {} (user={}, bucket_url={})", dealId, uploadedByUserId, bucketUrl);
    }

    /**
     * Marks the deal as {@code CLASSIFIED} in the database.
     *
     * @param dealId          the deal to update
     * @param classifiedStatus the status string emitted by the FastAPI worker (e.g. "CLASSIFIED")
     * @throws IllegalArgumentException if the deal_id is not found (prevents orphan state updates)
     */
    @Transactional
    public void markClassified(String dealId, String classifiedStatus) {
        Objects.requireNonNull(dealId, "dealId must not be null");
        DealState deal = dealStateRepository.findById(dealId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Unknown deal_id — cannot update classification status: " + dealId));

        log.info("Updating deal {} status: {} -> {}", dealId, deal.getStatus(), classifiedStatus);
        deal.setStatus(classifiedStatus);
        dealStateRepository.save(deal);
        log.info("Deal {} successfully transitioned to status={}", dealId, classifiedStatus);
    }
}
