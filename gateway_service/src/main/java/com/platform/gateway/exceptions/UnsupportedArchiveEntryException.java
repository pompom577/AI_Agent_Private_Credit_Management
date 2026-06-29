package com.platform.gateway.exceptions;

/**
 * Thrown by {@code ZipInspectionService.inspectEntries} when the archive contains
 * disallowed file types (e.g., .sh, .bat, .exe). Mapped to HTTP 415 Unsupported
 * Media Type by {@link GlobalExceptionHandler}.
 */
public class UnsupportedArchiveEntryException extends RuntimeException {

    public UnsupportedArchiveEntryException(String message) {
        super(message);
    }
}
