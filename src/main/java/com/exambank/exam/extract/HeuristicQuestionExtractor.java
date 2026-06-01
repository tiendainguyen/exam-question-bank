package com.exambank.exam.extract;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.exambank.common.exception.UnsupportedExamFormatException;

import org.springframework.stereotype.Component;

/**
 * Free, deterministic extractor. Strips the "Câu N" marker, then parses A/B/C/D
 * choices via regex. Throws {@link UnsupportedExamFormatException} when the file
 * has no markers or no parseable choices — the caller can then retry with AI.
 */
@Component
public class HeuristicQuestionExtractor implements QuestionExtractor {

    private static final Pattern MARKER = Pattern.compile("(?iu)^\\s*c[aâ]u\\s+\\d+\\s*[.:)]?\\s*");
    private static final Pattern CHOICE = Pattern.compile("(?m)(?:^|\\s)([A-D])[.)]\\s+");

    @Override
    public List<ExtractedQuestion> extract(List<QuestionBlock> blocks) {
        if (blocks.isEmpty()) {
            throw new UnsupportedExamFormatException(
                    "No \"Câu N\" markers found — the file format is not standard.");
        }

        List<ExtractedQuestion> result = new ArrayList<>(blocks.size());
        int totalChoices = 0;
        for (QuestionBlock block : blocks) {
            String body = MARKER.matcher(block.text()).replaceFirst("");
            ParsedChoices parsed = parseChoices(body);
            totalChoices += parsed.choices().size();
            result.add(new ExtractedQuestion(block.ordinal(), parsed.stem(), parsed.choices(), null));
        }

        if (totalChoices == 0) {
            throw new UnsupportedExamFormatException(
                    "Questions found but no A/B/C/D choices could be parsed — the format is not standard.");
        }
        return result;
    }

    private ParsedChoices parseChoices(String body) {
        Matcher matcher = CHOICE.matcher(body);
        List<Integer> labelStarts = new ArrayList<>();
        List<Integer> contentStarts = new ArrayList<>();
        while (matcher.find()) {
            labelStarts.add(matcher.start(1));
            contentStarts.add(matcher.end());
        }
        if (labelStarts.isEmpty()) {
            return new ParsedChoices(body.strip(), List.of());
        }
        String stem = body.substring(0, labelStarts.get(0)).strip();
        List<String> choices = new ArrayList<>(labelStarts.size());
        for (int i = 0; i < labelStarts.size(); i++) {
            int from = contentStarts.get(i);
            int to = (i + 1 < labelStarts.size()) ? labelStarts.get(i + 1) : body.length();
            choices.add(body.substring(from, to).strip());
        }
        return new ParsedChoices(stem, choices);
    }

    private record ParsedChoices(String stem, List<String> choices) {
    }
}
