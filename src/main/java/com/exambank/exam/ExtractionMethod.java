package com.exambank.exam;

/** How an exam's questions are extracted from raw text. */
public enum ExtractionMethod {
    /** Deterministic regex parser — free, no LLM, but expects a standard format. */
    HEURISTIC,
    /** LLM-backed — robust to messy formats, needs a configured model/API key. */
    AI
}
