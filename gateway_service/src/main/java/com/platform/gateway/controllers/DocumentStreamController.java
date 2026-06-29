package com.platform.gateway.controllers;

import com.platform.gateway.services.StorageStreamService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

/**
 * GET /documents/{sourceDocId}/page/{pageNumber}
 *
 * Streams the requested PDF page bytes from S3 directly to the client without buffering
 * the full file in JVM heap (Sub-Story 2.2a, TC-BE-01 / TC-BE-02).
 *
 * Error paths (404 / 422 / 504) are thrown by {@link StorageStreamService#openStream} before
 * the first byte is written, keeping the HTTP status uncommitted and delegatable to
 * {@link com.platform.gateway.exceptions.GlobalExceptionHandler}.
 */
@RestController
@RequestMapping("/documents")
public class DocumentStreamController {

    private final StorageStreamService storageStreamService;

    public DocumentStreamController(StorageStreamService storageStreamService) {
        this.storageStreamService = storageStreamService;
    }

    @GetMapping("/{sourceDocId}/page/{pageNumber}")
    public ResponseEntity<StreamingResponseBody> streamPage(
            @PathVariable Long sourceDocId,
            @PathVariable int pageNumber) {

        // openStream validates existence and page bounds; throws before writing any bytes.
        ResponseInputStream<GetObjectResponse> s3Stream =
                storageStreamService.openStream(sourceDocId, pageNumber);

        StreamingResponseBody body = outputStream -> {
            try (s3Stream) {
                s3Stream.transferTo(outputStream);
            }
        };

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .body(body);
    }
}
