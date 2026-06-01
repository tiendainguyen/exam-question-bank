package com.exambank.common.exception;

/** Thrown when reading, parsing, or extracting an exam document fails. */
public class DocumentProcessingException extends RuntimeException {

    public DocumentProcessingException(String message) {
        super(message);
    }

    public DocumentProcessingException(String message, Throwable cause) {
        super(message, cause);
    }
}
