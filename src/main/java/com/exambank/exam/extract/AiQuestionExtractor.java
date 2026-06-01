package com.exambank.exam.extract;

import java.util.List;
import java.util.stream.Collectors;

import com.exambank.common.exception.DocumentProcessingException;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

/**
 * LLM-backed extractor using Spring AI structured output. The
 * {@code ChatClient.Builder} is resolved lazily via {@link ObjectProvider} so
 * the application still boots when no model/API key is configured — extraction
 * just fails at call time with a clear message.
 */
@Component
public class AiQuestionExtractor implements QuestionExtractor {

    private static final String SYSTEM = """
            You extract exam questions from raw text into structured data.
            For each numbered question return: its ordinal (the "Câu N" number),
            the full question stem, the list of answer choices (empty if none),
            and the correct answer if explicitly stated (otherwise null).
            Preserve the original language. Never invent questions or choices.""";

    private final ObjectProvider<ChatClient.Builder> chatClientBuilder;

    public AiQuestionExtractor(ObjectProvider<ChatClient.Builder> chatClientBuilder) {
        this.chatClientBuilder = chatClientBuilder;
    }

    @Override
    public List<ExtractedQuestion> extract(List<QuestionBlock> blocks) {
        if (blocks.isEmpty()) {
            return List.of();
        }
        ChatClient.Builder builder = chatClientBuilder.getIfAvailable();
        if (builder == null) {
            throw new DocumentProcessingException(
                    "LLM is not configured — set ANTHROPIC_API_KEY to enable extraction");
        }
        String userText = blocks.stream()
                .map(b -> "Câu " + b.ordinal() + ": " + b.text())
                .collect(Collectors.joining("\n\n"));

        try {
            ExtractedExam result = builder.build()
                    .prompt()
                    .system(SYSTEM)
                    .user(userText)
                    .call()
                    .entity(ExtractedExam.class);
            return result == null ? List.of() : result.questions();
        } catch (RuntimeException e) {
            throw new DocumentProcessingException("AI extraction failed: " + e.getMessage(), e);
        }
    }
}
