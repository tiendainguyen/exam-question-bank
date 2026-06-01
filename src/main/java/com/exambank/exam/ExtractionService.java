package com.exambank.exam;

import java.awt.image.BufferedImage;
import java.util.List;
import java.util.UUID;

import com.exambank.audit.ExtractionAuditLogger;
import com.exambank.exam.extract.ExtractedQuestion;
import com.exambank.exam.extract.HeuristicQuestionExtractor;
import com.exambank.exam.extract.PdfPageRenderer;
import com.exambank.exam.extract.PdfTextExtractor;
import com.exambank.exam.extract.QuestionBlock;
import com.exambank.exam.extract.QuestionBlockSplitter;
import com.exambank.exam.extract.TesseractOcrEngine;
import com.exambank.exam.extract.VisionQuestionExtractor;
import com.exambank.security.UserContext;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Orchestrates extraction. Two OCR-capable methods:
 * <ul>
 *   <li><b>TESSERACT</b> — try the embedded text layer, fall back to rasterize +
 *       Tesseract OCR when empty, then split "Câu N" blocks and parse with the
 *       heuristic extractor.</li>
 *   <li><b>AI_VISION</b> — render pages to images and let a vision LLM return
 *       structured questions directly.</li>
 * </ul>
 * Idempotent: re-extracting replaces the prior questions for the exam.
 */
@Service
@Slf4j
public class ExtractionService {

    private final ExamService examService;
    private final ExamStorage examStorage;
    private final PdfTextExtractor pdfTextExtractor;
    private final PdfPageRenderer pageRenderer;
    private final TesseractOcrEngine tesseractOcr;
    private final QuestionBlockSplitter splitter;
    private final HeuristicQuestionExtractor heuristicExtractor;
    private final VisionQuestionExtractor visionExtractor;
    private final QuestionRepository questionRepository;
    private final ExtractionAuditLogger auditLogger;
    private final int textLayerMinChars;

    public ExtractionService(ExamService examService, ExamStorage examStorage,
            PdfTextExtractor pdfTextExtractor, PdfPageRenderer pageRenderer,
            TesseractOcrEngine tesseractOcr, QuestionBlockSplitter splitter,
            HeuristicQuestionExtractor heuristicExtractor, VisionQuestionExtractor visionExtractor,
            QuestionRepository questionRepository, ExtractionAuditLogger auditLogger,
            @Value("${app.ocr.text-layer-min-chars:40}") int textLayerMinChars) {
        this.examService = examService;
        this.examStorage = examStorage;
        this.pdfTextExtractor = pdfTextExtractor;
        this.pageRenderer = pageRenderer;
        this.tesseractOcr = tesseractOcr;
        this.splitter = splitter;
        this.heuristicExtractor = heuristicExtractor;
        this.visionExtractor = visionExtractor;
        this.questionRepository = questionRepository;
        this.auditLogger = auditLogger;
        this.textLayerMinChars = textLayerMinChars;
    }

    @Transactional
    public List<Question> extract(UUID examId, ExtractionMethod method) {
        examService.requireOwned(examId); // ownership + existence check
        UUID userId = UserContext.getRequired(); // capture on request thread for the async audit
        log.debug("extract:start examId={} method={} user={}", examId, method, userId);

        byte[] pdf = examStorage.read(examId);
        log.debug("extract:read-pdf examId={} bytes={}", examId, pdf.length);

        List<ExtractedQuestion> extracted = switch (method) {
            case TESSERACT -> extractWithTesseract(examId, pdf);
            case AI_VISION -> extractWithVision(examId, pdf);
        };
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

    /** Text-layer first; OCR with Tesseract only when the layer is empty (a scan). */
    private List<ExtractedQuestion> extractWithTesseract(UUID examId, byte[] pdf) {
        String text = pdfTextExtractor.extractText(pdf);
        log.debug("extract:pdf->text examId={} chars={}", examId, text.length());

        if (text.strip().length() < textLayerMinChars) {
            log.debug("extract:text-layer-empty examId={} -> tesseract OCR", examId);
            List<BufferedImage> pages = pageRenderer.render(pdf);
            text = tesseractOcr.ocr(pages);
            log.debug("extract:ocr examId={} pages={} chars={}", examId, pages.size(), text.length());
        }

        List<QuestionBlock> blocks = splitter.split(text);
        log.debug("extract:split examId={} blocks={}", examId, blocks.size());
        return heuristicExtractor.extract(blocks);
    }

    /** Render pages to images and let the vision model return questions directly. */
    private List<ExtractedQuestion> extractWithVision(UUID examId, byte[] pdf) {
        List<BufferedImage> pages = pageRenderer.render(pdf);
        log.debug("extract:render examId={} pages={}", examId, pages.size());
        return visionExtractor.extract(pages);
    }
}