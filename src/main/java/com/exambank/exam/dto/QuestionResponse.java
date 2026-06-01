package com.exambank.exam.dto;

import java.util.List;
import java.util.UUID;

import com.exambank.exam.Question;

public record QuestionResponse(
        UUID id,
        int ordinal,
        String stem,
        List<String> choices,
        String correctAnswer) {

    public static QuestionResponse from(Question question) {
        return new QuestionResponse(
                question.getId(),
                question.getOrdinal(),
                question.getStem(),
                question.getChoices(),
                question.getCorrectAnswer());
    }
}
