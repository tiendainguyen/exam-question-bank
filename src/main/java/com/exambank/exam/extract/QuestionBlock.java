package com.exambank.exam.extract;

/** A raw slice of exam text for one question, keyed by its "Câu N" ordinal. */
public record QuestionBlock(int ordinal, String text) {
}
