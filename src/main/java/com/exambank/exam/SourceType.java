package com.exambank.exam;

/** Origin of an {@link ExamPaper}. */
public enum SourceType {
    /** Uploaded by a user as the reference/illustrative exam. */
    ILLUSTRATIVE,
    /** Produced by the system from the question bank (B5). */
    GENERATED,
    /** A paper from the shared question bank (B4). */
    BANK
}
