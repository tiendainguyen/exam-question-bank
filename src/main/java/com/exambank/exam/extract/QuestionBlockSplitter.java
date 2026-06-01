package com.exambank.exam.extract;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

/**
 * Splits raw exam text into per-question blocks on "Câu N" markers
 * (case- and diacritic-tolerant). Deterministic — no LLM. Each block keeps its
 * marker line so the downstream extractor sees full context.
 */
@Component
public class QuestionBlockSplitter {

    private static final Pattern MARKER = Pattern.compile(
            "(?imu)^[ \\t]*c[aâ]u[ \\t]+(\\d+)[ \\t]*[.:)]?");

    public List<QuestionBlock> split(String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }
        Matcher matcher = MARKER.matcher(text);
        List<Integer> starts = new ArrayList<>();
        List<Integer> ordinals = new ArrayList<>();
        while (matcher.find()) {
            starts.add(matcher.start());
            ordinals.add(Integer.parseInt(matcher.group(1)));
        }

        List<QuestionBlock> blocks = new ArrayList<>(starts.size());
        for (int i = 0; i < starts.size(); i++) {
            int from = starts.get(i);
            int to = (i + 1 < starts.size()) ? starts.get(i + 1) : text.length();
            blocks.add(new QuestionBlock(ordinals.get(i), text.substring(from, to).strip()));
        }
        return blocks;
    }
}
