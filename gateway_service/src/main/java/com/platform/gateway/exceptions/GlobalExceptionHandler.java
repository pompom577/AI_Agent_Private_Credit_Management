package com.platform.gateway.exceptions;

import com.platform.gateway.dto.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;

import java.util.NoSuchElementException;

/**
 * Translates domain exceptions into the HTTP error contract from the 1.1b diagrams.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(InvalidArchiveFormatException.class)
    public ResponseEntity<ErrorResponse> handleInvalidArchiveFormat(InvalidArchiveFormatException ex) {
        return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
                .body(new ErrorResponse("invalid zip format"));
    }

    @ExceptionHandler(UnsupportedArchiveEntryException.class)
    public ResponseEntity<ErrorResponse> handleUnsupportedEntries(UnsupportedArchiveEntryException ex) {
        return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
                .body(new ErrorResponse("unsupported entries"));
    }

    @ExceptionHandler(EncryptedArchiveException.class)
    public ResponseEntity<ErrorResponse> handleEncryptedArchive(EncryptedArchiveException ex) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(new ErrorResponse("encrypted archive"));
    }

    @ExceptionHandler(ClassificationHandoffException.class)
    public ResponseEntity<ErrorResponse> handleClassificationHandoff(ClassificationHandoffException ex) {
        return ResponseEntity.status(ex.getStatus())
                .body(new ErrorResponse("classification handoff failed: " + ex.getStatus().value()));
    }

    // 413: archive exceeds gateway.upload.max-size-bytes / spring.servlet.multipart.max-*.
    // Declared before MultipartException since MaxUploadSizeExceededException extends it.
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorResponse> handleMaxUploadSize(MaxUploadSizeExceededException ex) {
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
                .body(new ErrorResponse("upload too large"));
    }

    @ExceptionHandler(InvalidPageRangeException.class)
    public ResponseEntity<ErrorResponse> handleInvalidPageRange(InvalidPageRangeException ex) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(new ErrorResponse(ex.getMessage()));
    }

    @ExceptionHandler(DocumentStreamTimeoutException.class)
    public ResponseEntity<ErrorResponse> handleDocumentStreamTimeout(DocumentStreamTimeoutException ex) {
        return ResponseEntity.status(HttpStatus.GATEWAY_TIMEOUT)
                .body(new ErrorResponse(ex.getMessage()));
    }

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(NoSuchElementException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse(ex.getMessage()));
    }

    // 400: malformed multipart payload (missing boundary, truncated stream, etc.).
    @ExceptionHandler(MultipartException.class)
    public ResponseEntity<ErrorResponse> handleMultipart(MultipartException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse("invalid multipart request"));
    }
}
