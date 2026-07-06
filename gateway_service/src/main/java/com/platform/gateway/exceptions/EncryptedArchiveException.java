package com.platform.gateway.exceptions;

/**
 * Thrown by {@code ZipInspectionService.assertNotEncrypted} when the uploaded archive
 * is password-protected. Mapped to HTTP 422 Unprocessable Entity by
 * {@link GlobalExceptionHandler}.
 */
public class EncryptedArchiveException extends RuntimeException {

    public EncryptedArchiveException(String message) {
        super(message);
    }
}
