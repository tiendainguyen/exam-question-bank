package com.exambank.exam.extract;

import java.util.List;

import com.exambank.common.exception.UnsupportedExamFormatException;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class HeuristicQuestionExtractorTest {

    private final HeuristicQuestionExtractor extractor = new HeuristicQuestionExtractor();

    @Test
    void parsesStemAndInlineChoices() {
        List<QuestionBlock> blocks = List.of(
                new QuestionBlock(1, "Câu 1: Choose the comparative.\nA. tall B. taller C. tallest D. more tall"));

        List<ExtractedQuestion> questions = extractor.extract(blocks);

        assertThat(questions).hasSize(1);
        ExtractedQuestion q = questions.get(0);
        assertThat(q.ordinal()).isEqualTo(1);
        assertThat(q.stem()).isEqualTo("Choose the comparative.");
        assertThat(q.choices()).containsExactly("tall", "taller", "tallest", "more tall");
    }

    @Test
    void throwsWhenNoChoicesParseable() {
        List<QuestionBlock> blocks = List.of(
                new QuestionBlock(1, "Câu 1: a prompt with no options at all"));

        assertThatThrownBy(() -> extractor.extract(blocks))
                .isInstanceOf(UnsupportedExamFormatException.class);
    }

    @Test
    void throwsWhenNoBlocks() {
        assertThatThrownBy(() -> extractor.extract(List.of()))
                .isInstanceOf(UnsupportedExamFormatException.class);
    }
}
