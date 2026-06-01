package com.exambank.exam.extract;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;

import com.exambank.common.exception.DocumentProcessingException;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Rasterizes PDF pages to images so a scanned PDF (no text layer) can be OCR'd —
 * either by Tesseract or by a vision model. DPI is configurable: higher = better
 * OCR accuracy but larger images / more tokens.
 */
@Component
public class PdfPageRenderer {

    private final int dpi;

    public PdfPageRenderer(@Value("${app.ocr.render-dpi:200}") int dpi) {
        this.dpi = dpi;
    }

    /** One {@link BufferedImage} per page, rendered at the configured DPI. */
    public List<BufferedImage> render(byte[] pdf) {
        try (PDDocument doc = Loader.loadPDF(pdf)) {
            PDFRenderer renderer = new PDFRenderer(doc);
            int pageCount = doc.getNumberOfPages();
            List<BufferedImage> images = new ArrayList<>(pageCount);
            for (int i = 0; i < pageCount; i++) {
                images.add(renderer.renderImageWithDPI(i, dpi, ImageType.RGB));
            }
            return images;
        } catch (IOException e) {
            throw new DocumentProcessingException("Failed to render PDF pages to images", e);
        }
    }

    /** Encodes a rendered page to PNG bytes (for sending to a vision model). */
    public static byte[] toPng(BufferedImage image) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ImageIO.write(image, "png", out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new DocumentProcessingException("Failed to encode page image as PNG", e);
        }
    }
}