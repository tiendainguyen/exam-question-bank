package com.exambank.exam.extract;

import java.io.IOException;

import com.exambank.common.exception.DocumentProcessingException;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;

/** Extracts plain text from a PDF byte stream using Apache PDFBox. */
@Component
public class PdfTextExtractor {

    public String extractText(byte[] pdf) {
        try (PDDocument document = Loader.loadPDF(pdf)) {
            return new PDFTextStripper().getText(document);
        } catch (IOException e) {
            throw new DocumentProcessingException("Failed to read PDF", e);
        }
    }
}
