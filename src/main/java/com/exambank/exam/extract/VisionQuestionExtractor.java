package com.exambank.exam.extract;

import java.awt.image.BufferedImage;
import java.util.List;

import com.exambank.common.exception.DocumentProcessingException;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.content.Media;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Component;
import org.springframework.util.MimeTypeUtils;

/**
 * Vision-LLM extractor: sends rendered page images to a multimodal model and
 * gets structured questions back directly — no text layer, no regex. Needs a
 * vision-capable model (the default Groq text model cannot see images). The
 * {@code ChatClient.Builder} is resolved lazily so the app still boots without
 * a configured key.
 */
@Component
public class VisionQuestionExtractor {

    private static final String SYSTEM = """
            You read images of exam pages and extract every question into structured data.
            For each question return: its ordinal (the "Câu N" number, in order),
            the full question stem, the list of answer choices (empty if none),
            and the correct answer if explicitly marked (otherwise null).
            Preserve the original language. Never invent questions or choices.""";

    private static final String USER = "Extract all questions from these exam page images.";

    private final ObjectProvider<ChatClient.Builder> chatClientBuilder;

    public VisionQuestionExtractor(ObjectProvider<ChatClient.Builder> chatClientBuilder) {
        this.chatClientBuilder = chatClientBuilder;
    }

    public List<ExtractedQuestion> extract(List<BufferedImage> pages) {
        if (pages.isEmpty()) {
            return List.of();
        }
        ChatClient.Builder builder = chatClientBuilder.getIfAvailable();
        if (builder == null) {
            throw new DocumentProcessingException(
                    "Vision model is not configured — set AI_API_KEY and a vision-capable AI_MODEL");
        }
        Media[] media = pages.stream()
                .map(page -> new Media(MimeTypeUtils.IMAGE_PNG,
                        new ByteArrayResource(PdfPageRenderer.toPng(page))))
                .toArray(Media[]::new);
        try {
            ExtractedExam result = builder.build()
                    .prompt()
                    .system(SYSTEM)
                    .user(u -> u.text(USER).media(media))
                    .call()
                    .entity(ExtractedExam.class);
            return result == null ? List.of() : result.questions();
        } catch (RuntimeException e) {
            throw new DocumentProcessingException("AI vision extraction failed: " + e.getMessage(), e);
        }
    }
}