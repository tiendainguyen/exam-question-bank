package com.exambank.audit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.util.UUID;

import com.exambank.exam.ExtractionMethod;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class ExtractionAuditLoggerTest {

    private final ExtractionLogRepository repository = mock(ExtractionLogRepository.class);
    private final ExtractionAuditLogger logger = new ExtractionAuditLogger(repository);

    @Test
    void persists_row_capturing_user_exam_method_and_count() {
        UUID user = UUID.randomUUID();
        UUID exam = UUID.randomUUID();

        logger.logExtractionAsync(user, exam, ExtractionMethod.AI, 42);

        ArgumentCaptor<ExtractionLog> captor = ArgumentCaptor.forClass(ExtractionLog.class);
        verify(repository).save(captor.capture());
        ExtractionLog row = captor.getValue();
        assertThat(row.getUserId()).isEqualTo(user);
        assertThat(row.getExamPaperId()).isEqualTo(exam);
        assertThat(row.getMethod()).isEqualTo(ExtractionMethod.AI);
        assertThat(row.getQuestionCount()).isEqualTo(42);
        assertThat(row.getId()).isNotNull();
        assertThat(row.getCreatedAt()).isNotNull();
    }

    @Test
    void swallows_repository_failure_so_extraction_is_never_broken() {
        doThrow(new RuntimeException("db down")).when(repository).save(any());

        assertThatCode(() -> logger.logExtractionAsync(
                UUID.randomUUID(), UUID.randomUUID(), ExtractionMethod.HEURISTIC, 1))
                .doesNotThrowAnyException();
    }
}
