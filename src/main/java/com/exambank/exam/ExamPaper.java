package com.exambank.exam;

import java.time.Instant;
import java.util.UUID;

import com.exambank.common.OwnedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** An exam paper owned by a user. */
@Entity
@Table(name = "exam_paper")
public class ExamPaper extends OwnedEntity {

    @Id
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false)
    private SourceType sourceType;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected ExamPaper() {
        // for JPA
    }

    public ExamPaper(UUID id, UUID userId, String name, SourceType sourceType, Instant createdAt) {
        this.id = id;
        setUserId(userId);
        this.name = name;
        this.sourceType = sourceType;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public SourceType getSourceType() {
        return sourceType;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
