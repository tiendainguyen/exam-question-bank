package com.exambank.audit;

import java.time.Instant;
import java.util.UUID;

import com.exambank.exam.ExtractionMethod;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Immutable audit record of a single extraction run. Written by
 * {@link ExtractionAuditLogger} on a background thread — purely a server-side
 * trail, never returned to clients. Not an {@code OwnedEntity}: it is an audit
 * fact, not personalized read data, so it is exempt from per-user read filters.
 */
@Entity
@Table(name = "extraction_log")
public class ExtractionLog {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Column(name = "exam_paper_id", nullable = false, updatable = false)
    private UUID examPaperId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false)
    private ExtractionMethod method;

    @Column(name = "question_count", nullable = false, updatable = false)
    private int questionCount;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected ExtractionLog() {
        // for JPA
    }

    public ExtractionLog(UUID id, UUID userId, UUID examPaperId, ExtractionMethod method,
            int questionCount, Instant createdAt) {
        this.id = id;
        this.userId = userId;
        this.examPaperId = examPaperId;
        this.method = method;
        this.questionCount = questionCount;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public UUID getExamPaperId() {
        return examPaperId;
    }

    public ExtractionMethod getMethod() {
        return method;
    }

    public int getQuestionCount() {
        return questionCount;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
