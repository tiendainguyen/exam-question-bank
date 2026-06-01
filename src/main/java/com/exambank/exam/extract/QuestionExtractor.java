package com.exambank.exam.extract;

import java.util.List;

/**
 * Turns raw question blocks into structured questions. Abstracted behind an
 * interface so the LLM-backed implementation can be swapped for a stub in tests.
 */
public interface QuestionExtractor {

    List<ExtractedQuestion> extract(List<QuestionBlock> blocks);
}
