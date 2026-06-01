package com.exambank.audit;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ExtractionLogRepository extends JpaRepository<ExtractionLog, UUID> {
}
