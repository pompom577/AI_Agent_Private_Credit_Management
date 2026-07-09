package com.platform.gateway.exceptions;

public class DocumentStreamTimeoutException extends RuntimeException {

    public DocumentStreamTimeoutException(Long docId) {
        super("Storage fetch timed out for document " + docId);
    }
}
