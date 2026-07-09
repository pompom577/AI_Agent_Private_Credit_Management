package com.platform.gateway.exceptions;

/**
 * Thrown by {@code ZipInspectionService.validateMagicBytes} when the uploaded bytes
 * do not start with a valid ZIP local-file-header signature (e.g., a {@code .exe}
 * renamed to {@code .zip}). Mapped to HTTP 415 Unsupported Media Type by
 * {@link GlobalExceptionHandler}.
 *
 * Covers TC-GW-02.
 */
public class InvalidArchiveFormatException extends RuntimeException {

    public InvalidArchiveFormatException(String message) {
        super(message);
    }
}
