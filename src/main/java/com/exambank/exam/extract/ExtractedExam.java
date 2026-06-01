package com.exambank.exam.extract;

import java.util.List;

/** Structured-output target for the LLM: the full list of extracted questions. */
public record ExtractedExam(List<ExtractedQuestion> questions) {
}
