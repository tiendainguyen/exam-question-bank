package com.exambank.exam;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ExamPaperRepository extends JpaRepository<ExamPaper, UUID> {

    /** Ownership-scoped lookup — the only way services should fetch an exam. */
    Optional<ExamPaper> findByIdAndUserId(UUID id, UUID userId);
}
