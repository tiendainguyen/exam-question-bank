package com.exambank.exam;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import com.exambank.common.exception.DocumentProcessingException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Stores and reads uploaded exam PDFs on the local filesystem. */
@Component
public class ExamStorage {

    private final Path dir;

    public ExamStorage(@Value("${app.storage.exam-dir}") String dir) {
        this.dir = Path.of(dir);
    }

    public void store(UUID examId, byte[] content) {
        try {
            Files.createDirectories(dir);
            Files.write(dir.resolve(examId + ".pdf"), content);
        } catch (IOException e) {
            throw new DocumentProcessingException("Failed to store exam file", e);
        }
    }

    public byte[] read(UUID examId) {
        try {
            return Files.readAllBytes(dir.resolve(examId + ".pdf"));
        } catch (IOException e) {
            throw new DocumentProcessingException("Failed to read exam file", e);
        }
    }
}
