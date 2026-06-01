package com.exambank.exam;

import java.util.List;
import java.util.UUID;

import com.exambank.audit.ExtractionAuditLogger;
import com.exambank.exam.extract.AiQuestionExtractor;
import com.exambank.exam.extract.ExtractedQuestion;
import com.exambank.exam.extract.HeuristicQuestionExtractor;
import com.exambank.exam.extract.PdfTextExtractor;
import com.exambank.exam.extract.QuestionBlock;
import com.exambank.exam.extract.QuestionBlockSplitter;
import com.exambank.exam.extract.QuestionExtractor;
import com.exambank.security.UserContext;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Orchestrates extraction: read stored PDF → text → split into "Câu N" blocks →
 * LLM structured output → persist {@link Question}s. Idempotent: re-extracting
 * replaces the prior questions for the exam.
 */
@Service
@Slf4j
public class ExtractionService {

    private final ExamService examService;
    private final ExamStorage examStorage;
    private final PdfTextExtractor pdfTextExtractor;
    private final QuestionBlockSplitter splitter;
    private final HeuristicQuestionExtractor heuristicExtractor;
    private final AiQuestionExtractor aiExtractor;
    private final QuestionRepository questionRepository;
    private final ExtractionAuditLogger auditLogger;

    public ExtractionService(ExamService examService, ExamStorage examStorage,
            PdfTextExtractor pdfTextExtractor, QuestionBlockSplitter splitter,
            HeuristicQuestionExtractor heuristicExtractor, AiQuestionExtractor aiExtractor,
            QuestionRepository questionRepository, ExtractionAuditLogger auditLogger) {
        this.examService = examService;
        this.examStorage = examStorage;
        this.pdfTextExtractor = pdfTextExtractor;
        this.splitter = splitter;
        this.heuristicExtractor = heuristicExtractor;
        this.aiExtractor = aiExtractor;
        this.questionRepository = questionRepository;
        this.auditLogger = auditLogger;
    }

    @Transactional
    public List<Question> extract(UUID examId, ExtractionMethod method) {
        examService.requireOwned(examId); // ownership + existence check
        UUID userId = UserContext.getRequired(); // capture on request thread for the async audit
        log.debug("extract:start examId={} method={} user={}", examId, method, userId);

        byte[] pdf = examStorage.read(examId);
        log.debug("extract:read-pdf examId={} bytes={}", examId, pdf.length);

        String text = pdfTextExtractor.extractText(pdf);
        log.debug("extract:pdf->text examId={} chars={}", examId, text.length());

        List<QuestionBlock> blocks = splitter.split(text);
        log.debug("extract:split examId={} blocks={}", examId, blocks.size());

        QuestionExtractor extractor = (method == ExtractionMethod.AI) ? aiExtractor : heuristicExtractor;
        log.debug("extract:extractor-chosen examId={} extractor={}", examId, extractor.getClass().getSimpleName());

        List<ExtractedQuestion> extracted = extractor.extract(blocks);
        log.debug("extract:parsed examId={} questions={}", examId, extracted.size());

        questionRepository.deleteByExamPaperId(examId);
        List<Question> toSave = extracted.stream()
                .map(eq -> new Question(UUID.randomUUID(), examId, eq.ordinal(),
                        eq.stem(), eq.choices(), eq.correctAnswer(), null))
                .toList();
        List<Question> saved = questionRepository.saveAll(toSave);
        log.info("extract:done examId={} method={} saved={}", examId, method, saved.size());

        // Server-side audit only — fire-and-forget, nothing surfaced to the UI.
        auditLogger.logExtractionAsync(userId, examId, method, saved.size());
        return saved;
    }
}
