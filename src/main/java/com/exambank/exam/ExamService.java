package com.exambank.exam;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.exambank.common.exception.ExamNotFoundException;
import com.exambank.security.UserContext;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ExamService {

    private final ExamPaperRepository examPaperRepository;
    private final QuestionRepository questionRepository;
    private final ExamStorage examStorage;

    public ExamService(ExamPaperRepository examPaperRepository,
            QuestionRepository questionRepository, ExamStorage examStorage) {
        this.examPaperRepository = examPaperRepository;
        this.questionRepository = questionRepository;
        this.examStorage = examStorage;
    }

    @Transactional
    public ExamPaper createIllustrative(String name, byte[] pdf) {
        UUID userId = UserContext.getRequired();
        UUID id = UUID.randomUUID();
        ExamPaper exam = new ExamPaper(id, userId, name, SourceType.ILLUSTRATIVE, Instant.now());
        examPaperRepository.save(exam);
        examStorage.store(id, pdf);
        return exam;
    }

    /** Fetches an exam scoped to the current user, or throws 404. */
    @Transactional(readOnly = true)
    public ExamPaper requireOwned(UUID examId) {
        return examPaperRepository.findByIdAndUserId(examId, UserContext.getRequired())
                .orElseThrow(() -> new ExamNotFoundException(examId));
    }

    @Transactional(readOnly = true)
    public List<Question> getQuestions(UUID examId) {
        requireOwned(examId);
        return questionRepository.findByExamPaperIdOrderByOrdinalAsc(examId);
    }
}
