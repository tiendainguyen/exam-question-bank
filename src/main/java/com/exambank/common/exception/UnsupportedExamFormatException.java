package com.exambank.common.exception;

/**
 * Thrown when the heuristic parser cannot make sense of an exam's format
 * (no "Câu N" markers, or no A/B/C/D choices). The API surfaces this with a
 * hint that the user can retry with AI extraction.
 */
public class UnsupportedExamFormatException extends RuntimeException {

    public UnsupportedExamFormatException(String message) {
        super(message);
    }
}
