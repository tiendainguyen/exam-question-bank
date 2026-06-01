package com.exambank.exam.dto;

import java.time.Instant;
import java.util.UUID;

import com.exambank.exam.ExamPaper;
import com.exambank.exam.SourceType;

public record ExamResponse(UUID id, String name, SourceType sourceType, Instant createdAt) {

    public static ExamResponse from(ExamPaper exam) {
        return new ExamResponse(exam.getId(), exam.getName(), exam.getSourceType(), exam.getCreatedAt());
    }
}
