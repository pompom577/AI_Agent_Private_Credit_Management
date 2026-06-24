package com.platform.gateway.services;

import com.platform.gateway.config.S3Config;
import com.platform.gateway.entities.DocumentRecord;
import com.platform.gateway.exceptions.DocumentStreamTimeoutException;
import com.platform.gateway.exceptions.InvalidPageRangeException;
import com.platform.gateway.repositories.DocumentRecordRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.exception.ApiCallAttemptTimeoutException;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.S3Client;
import java.util.NoSuchElementException;

/**
 * Resolves document metadata and opens a byte stream from S3 for Sub-Story 2.2a.
 *
 * The S3 request is initiated (and the 1.8-second timeout enforced) in {@link #openStream}
 * before the controller hands any bytes to the HTTP response — keeping the 504 path reachable
 * even after Spring commits the response status.
 */
@Service
public class StorageStreamService {

    private static final Logger log = LoggerFactory.getLogger(StorageStreamService.class);

    private final S3Client s3Client;
    private final S3Config s3Config;
    private final DocumentRecordRepository documentRecordRepository;

    @Value("${gateway.document.stream-timeout-ms:1800}")
    private long streamTimeoutMs;

    public StorageStreamService(S3Client s3Client,
                                S3Config s3Config,
                                DocumentRecordRepository documentRecordRepository) {
        this.s3Client = s3Client;
        this.s3Config = s3Config;
        this.documentRecordRepository = documentRecordRepository;
    }

    /**
     * Opens a streaming connection to the S3 object for the given document and page.
     *
     * Throws before writing any bytes, so the controller can still return a non-200 status on failure:
     * - {@link NoSuchElementException} → 404 (handled globally)
     * - {@link InvalidPageRangeException} → 422 (handled globally)
     * - {@link DocumentStreamTimeoutException} → 504 (handled globally)
     *
     * Callers must close the returned stream.
     */
    public ResponseInputStream<GetObjectResponse> openStream(Long sourceDocId, int pageNumber) {
        DocumentRecord doc = loadDocument(sourceDocId);

        if (pageNumber < 1 || (doc.getPageCount() != null && pageNumber > doc.getPageCount())) {
            throw new InvalidPageRangeException(pageNumber, sourceDocId);
        }

        String s3Key = resolveS3Key(doc.getFilePath());
        log.debug("Streaming document {} page {} from s3://{}/{}", sourceDocId, pageNumber,
                s3Config.getBucket(), s3Key);

        GetObjectRequest request = GetObjectRequest.builder()
                .bucket(s3Config.getBucket())
                .key(s3Key)
                .build();

        try {
            // s3Client is created with apiCallAttemptTimeout — see S3Config / application.yml.
            // The call returns once S3 sends response headers; the body is streamed lazily.
            return s3Client.getObject(request);
        } catch (ApiCallAttemptTimeoutException e) {
            log.warn("S3 fetch timed out for document {} after {}ms", sourceDocId, streamTimeoutMs);
            throw new DocumentStreamTimeoutException(sourceDocId);
        }
    }

    /**
     * Cached DB lookup for document metadata. Avoids repeated queries when an analyst
     * verifies multiple metrics from the same source document in one session.
     */
    @Cacheable(value = "document-records", unless = "#result == null")
    public DocumentRecord loadDocument(Long sourceDocId) {
        return documentRecordRepository.findById(sourceDocId)
                .orElseThrow(() -> new NoSuchElementException("Document not found: " + sourceDocId));
    }

    /**
     * Strips the "s3://bucket/" prefix from file_path if present, returning just the S3 object key.
     * Falls back to the raw value when file_path is already a bare key.
     */
    private String resolveS3Key(String filePath) {
        if (filePath != null && filePath.startsWith("s3://")) {
            // s3://bucket-name/key/path  →  key/path
            int firstSlash = filePath.indexOf('/', 5);   // skip "s3://"
            int secondSlash = filePath.indexOf('/', firstSlash + 1);
            return (secondSlash >= 0) ? filePath.substring(secondSlash + 1) : filePath;
        }
        return filePath;
    }
}
