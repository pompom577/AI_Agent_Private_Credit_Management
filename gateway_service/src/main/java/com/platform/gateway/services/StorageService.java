package com.platform.gateway.services;

import org.springframework.web.multipart.MultipartFile;

/**
 * Storage abstraction for the secure deal-ingestion bucket.
 *
 * TODO(1.1a): define final return type — currently a placeholder String (bucket URL).
 */
public interface StorageService {

    /**
     * Persists the validated archive into the secure bucket.
     *
     * @param dealId stable identifier for this deal (used as object key prefix).
     * @param file   validated multipart ZIP.
     * @return canonical bucket URL (e.g., {@code s3://bucket/deal_id/file.zip}).
     */
    String put(String dealId, MultipartFile file);
}
