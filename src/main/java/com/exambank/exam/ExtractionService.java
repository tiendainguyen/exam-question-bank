package com.exambank.exam;

import java.util.List;
import java.util.UUID;

import com.exambank.exam.extract.AiQuestionExtractor;
import com.exambank.exam.extract.ExtractedQuestion;
import com.exambank.exam.extract.HeuristicQuestionExtractor;
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
    private final HeuristicQuestionExtractor heuristicExtractor;
    private final AiQuestionExtractor aiExtractor;
    private final QuestionRepository questionRepository;

    public ExtractionService(ExamService examService, ExamStorage examStorage,
            PdfTextExtractor pdfTextExtractor, QuestionBlockSplitter splitter,
            HeuristicQuestionExtractor heuristicExtractor, AiQuestionExtractor aiExtractor,
            QuestionRepository questionRepository) {
        this.examService = examService;
        this.examStorage = examStorage;
        this.pdfTextExtractor = pdfTextExtractor;
        this.splitter = splitter;
        this.heuristicExtractor = heuristicExtractor;
        this.aiExtractor = aiExtractor;
        this.questionRepository = questionRepository;
    }

    @Transactional
    public List<Question> extract(UUID examId, ExtractionMethod method) {
        examService.requireOwned(examId); // ownership + existence check

        byte[] pdf = examStorage.read(examId);
        String text = pdfTextExtractor.extractText(pdf);
        List<QuestionBlock> blocks = splitter.split(text);
        QuestionExtractor extractor = (method == ExtractionMethod.AI) ? aiExtractor : heuristicExtractor;
        List<ExtractedQuestion> extracted = extractor.extract(blocks);

        questionRepository.deleteByExamPaperId(examId);
        List<Question> toSave = extracted.stream()
                .map(eq -> new Question(UUID.randomUUID(), examId, eq.ordinal(),
                        eq.stem(), eq.choices(), eq.correctAnswer(), null))
                .toList();
        return questionRepository.saveAll(toSave);
    }
}
