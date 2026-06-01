package com.exambank.exam.extract;

import java.awt.image.BufferedImage;
import java.util.List;

import com.exambank.common.exception.DocumentProcessingException;

import net.sourceforge.tess4j.ITesseract;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * OCR engine backed by Tesseract (tess4j). Requires native libtesseract +
 * tessdata installed on the host (e.g. {@code brew install tesseract
 * tesseract-lang}). A fresh {@link Tesseract} instance per call keeps it
 * thread-safe (tess4j instances are not safe to share across threads).
 */
@Component
public class TesseractOcrEngine {

    private final String dataPath;
    private final String language;

    public TesseractOcrEngine(
            @Value("${app.ocr.tessdata-path:}") String dataPath,
            @Value("${app.ocr.language:vie+eng}") String language) {
        this.dataPath = dataPath;
        this.language = language;
    }

    /** OCRs each page image and concatenates the recognized text. */
    public String ocr(List<BufferedImage> pages) {
        ITesseract tesseract = new Tesseract();
        if (StringUtils.hasText(dataPath)) {
            tesseract.setDatapath(dataPath); // else tess4j uses TESSDATA_PREFIX
        }
        tesseract.setLanguage(language);
        StringBuilder text = new StringBuilder();
        try {
            for (BufferedImage page : pages) {
                text.append(tesseract.doOCR(page)).append('\n');
            }
        } catch (TesseractException e) {
            throw new DocumentProcessingException(
                    "Tesseract OCR failed (is native tesseract installed?): " + e.getMessage(), e);
        }
        return text.toString();
    }
}