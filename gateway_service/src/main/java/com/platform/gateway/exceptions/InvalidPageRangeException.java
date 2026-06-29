package com.platform.gateway.exceptions;

public class InvalidPageRangeException extends RuntimeException {

    public InvalidPageRangeException(int pageNumber, Long docId) {
        super("page_number " + pageNumber + " is out of range for document " + docId);
    }
}
