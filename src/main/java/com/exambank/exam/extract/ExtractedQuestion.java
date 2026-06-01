package com.exambank.exam.extract;

import java.util.List;

/** One structured question produced by {@link QuestionExtractor}. */
public record ExtractedQuestion(
        int ordinal,
        String stem,
        List<String> choices,
        String correctAnswer) {
}
