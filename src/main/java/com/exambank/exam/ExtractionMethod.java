package com.exambank.exam;

/**
 * How an exam's questions are extracted from a PDF. Both options handle scanned
 * PDFs (no embedded text layer); they differ only in the OCR engine used.
 */
public enum ExtractionMethod {
    /**
     * OCR via Tesseract (tess4j). Tries the embedded text layer first and only
     * rasterizes + OCRs the pages when that layer is effectively empty. Free and
     * offline, but needs native tesseract installed on the host.
     */
    TESSERACT,
    /**
     * OCR via a vision LLM — renders pages to images and asks a multimodal model
     * for structured questions directly. Needs a vision-capable model configured.
     */
    AI_VISION
}