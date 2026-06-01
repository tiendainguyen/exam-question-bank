package com.exambank.exam.dto;

import java.util.List;
import java.util.UUID;

public record ExtractionResponse(UUID examId, int questionCount, List<QuestionResponse> questions) {
}
