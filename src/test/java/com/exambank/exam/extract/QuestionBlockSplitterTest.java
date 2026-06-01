package com.exambank.exam.extract;

import java.util.List;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class QuestionBlockSplitterTest {

    private final QuestionBlockSplitter splitter = new QuestionBlockSplitter();

    @Test
    void splitsOnCauMarkersAndKeepsOrdinals() {
        String text = """
                ĐỀ THI MINH HỌA
                Câu 1. Chọn dạng so sánh hơn nhất.
                A. tallest  B. taller
                Câu 2: Chuyển sang so sánh hơn.
                Câu 3) Điền từ thích hợp.
                """;

        List<QuestionBlock> blocks = splitter.split(text);

        assertThat(blocks).hasSize(3);
        assertThat(blocks).extracting(QuestionBlock::ordinal).containsExactly(1, 2, 3);
        assertThat(blocks.get(0).text()).startsWith("Câu 1");
        assertThat(blocks.get(0).text()).contains("A. tallest"); // body runs to next marker
    }

    @Test
    void isCaseAndDiacriticTolerant() {
        List<QuestionBlock> blocks = splitter.split("CAU 1 abc\ncâu 2 def\nCÂU 3 ghi");
        assertThat(blocks).extracting(QuestionBlock::ordinal).containsExactly(1, 2, 3);
    }

    @Test
    void splitsOnQuestionAndBaiMarkers() {
        String text = """
                Question 1: Choose the answer.
                A. a  B. b
                Question 2: Next one.
                Bài 3. Một câu hỏi.
                """;

        List<QuestionBlock> blocks = splitter.split(text);

        assertThat(blocks).extracting(QuestionBlock::ordinal).containsExactly(1, 2, 3);
        assertThat(blocks.get(0).text()).startsWith("Question 1");
    }

    @Test
    void emptyOrNullYieldsNoBlocks() {
        assertThat(splitter.split("")).isEmpty();
        assertThat(splitter.split("   ")).isEmpty();
        assertThat(splitter.split(null)).isEmpty();
    }

    @Test
    void textWithoutMarkersYieldsNoBlocks() {
        assertThat(splitter.split("Just some prose with no question markers.")).isEmpty();
    }
}
