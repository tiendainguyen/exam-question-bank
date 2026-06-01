package com.exambank.exam;

import java.util.List;
import java.util.UUID;

import com.exambank.exam.extract.ExtractedQuestion;
import com.exambank.exam.extract.PdfTextExtractor;
import com.exambank.exam.extract.QuestionBlock;
import com.exambank.exam.extract.QuestionBlockSplitter;
import com.exambank.exam.extract.QuestionExtractor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Orchestrates extraction: read stored PDF → text → split into "Câu N" blocks →
 * LLM structured output → persist {@link Question}s. Idempotent: re-extracting
 * replaces the prior questions for the exam.
 */
@Service
public class ExtractionService {

    private final ExamService examService;
    private final ExamStorage examStorage;
    private final PdfTextExtractor pdfTextExtractor;
    private final QuestionBlockSplitter splitter;
    private final QuestionExtractor questionExtractor;
    private final QuestionRepository questionRepository;

    public ExtractionService(ExamService examService, ExamStorage examStorage,
            PdfTextExtractor pdfTextExtractor, QuestionBlockSplitter splitter,
            QuestionExtractor questionExtractor, QuestionRepository questionRepository) {
        this.examService = examService;
        this.examStorage = examStorage;
        this.pdfTextExtractor = pdfTextExtractor;
        this.splitter = splitter;
        this.questionExtractor = questionExtractor;
        this.questionRepository = questionRepository;
    }

    @Transactional
    public List<Question> extract(UUID examId) {
        examService.requireOwned(examId); // ownership + existence check

        byte[] pdf = examStorage.read(examId);
        String text = pdfTextExtractor.extractText(pdf);
        List<QuestionBlock> blocks = splitter.split(text);
        List<ExtractedQuestion> extracted = questionExtractor.extract(blocks);

        questionRepository.deleteByExamPaperId(examId);
        List<Question> toSave = extracted.stream()
                .map(eq -> new Question(UUID.randomUUID(), examId, eq.ordinal(),
                        eq.stem(), eq.choices(), eq.correctAnswer(), null))
                .toList();
        return questionRepository.saveAll(toSave);
    }
}
