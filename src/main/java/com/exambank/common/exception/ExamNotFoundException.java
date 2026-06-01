package com.exambank.common.exception;

import java.util.UUID;

/** Thrown when an exam does not exist or is not owned by the current user. */
public class ExamNotFoundException extends RuntimeException {

    public ExamNotFoundException(UUID examId) {
        super("Exam not found: " + examId);
    }
}
