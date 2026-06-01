package com.exambank.exam;

import java.util.List;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * A question extracted from an {@link ExamPaper}. Owned transitively via its
 * exam — not an {@link com.exambank.common.OwnedEntity} itself.
 * {@code questionTypeId} is assigned in B2.
 */
@Entity
@Table(name = "question")
public class Question {

    @Id
    private UUID id;

    @Column(name = "exam_paper_id", nullable = false)
    private UUID examPaperId;

    @Column(nullable = false)
    private int ordinal;

    @Column(nullable = false, columnDefinition = "text")
    private String stem;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private List<String> choices;

    @Column(name = "correct_answer")
    private String correctAnswer;

    @Column(name = "question_type_id")
    private UUID questionTypeId;

    protected Question() {
        // for JPA
    }

    public Question(UUID id, UUID examPaperId, int ordinal, String stem,
            List<String> choices, String correctAnswer, UUID questionTypeId) {
        this.id = id;
        this.examPaperId = examPaperId;
        this.ordinal = ordinal;
        this.stem = stem;
        this.choices = choices;
        this.correctAnswer = correctAnswer;
        this.questionTypeId = questionTypeId;
    }

    public UUID getId() {
        return id;
    }

    public UUID getExamPaperId() {
        return examPaperId;
    }

    public int getOrdinal() {
        return ordinal;
    }

    public String getStem() {
        return stem;
    }

    public List<String> getChoices() {
        return choices;
    }

    public String getCorrectAnswer() {
        return correctAnswer;
    }

    public UUID getQuestionTypeId() {
        return questionTypeId;
    }
}
