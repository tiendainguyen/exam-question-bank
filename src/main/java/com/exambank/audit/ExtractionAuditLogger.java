package com.exambank.audit;

import java.time.Instant;
import java.util.UUID;

import com.exambank.exam.ExtractionMethod;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Fire-and-forget audit logger for extraction runs. Runs on the {@code
 * auditExecutor} pool in its own transaction so a slow or failing write never
 * blocks or breaks the user-facing extraction call. Identity is passed in as an
 * argument (captured on the request thread) — never read from {@code
 * UserContext} here, since this runs on a different thread.
 */
@Slf4j
@Service
public class ExtractionAuditLogger {

    private final ExtractionLogRepository repository;

    public ExtractionAuditLogger(ExtractionLogRepository repository) {
        this.repository = repository;
    }

    @Async("auditExecutor")
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logExtractionAsync(UUID userId, UUID examPaperId, ExtractionMethod method, int questionCount) {
        try {
            repository.save(new ExtractionLog(
                    UUID.randomUUID(), userId, examPaperId, method, questionCount, Instant.now()));
        } catch (Exception ex) {
            // Audit must never break extraction: swallow, just warn server-side.
            log.warn("extraction_audit_failure user={} exam={} method={} error={}",
                    userId, examPaperId, method, ex.getMessage());
        }
    }
}
