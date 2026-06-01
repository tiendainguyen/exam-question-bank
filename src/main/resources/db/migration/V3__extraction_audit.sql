-- B1: server-side audit trail for extraction runs (esp. AI-backed).
-- Records who triggered an extraction, on which exam, with which method, and how
-- many questions resulted. Written asynchronously; never surfaced to the UI.
-- No FK to exam_paper on purpose: the audit row must survive exam deletion.

CREATE TABLE extraction_log (
    id             UUID         PRIMARY KEY,
    user_id        UUID         NOT NULL,          -- who clicked extract
    exam_paper_id  UUID         NOT NULL,          -- which exam
    method         VARCHAR(16)  NOT NULL,          -- TESSERACT | AI_VISION
    question_count INT          NOT NULL,          -- questions produced
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX idx_extraction_log_user ON extraction_log (user_id);
CREATE INDEX idx_extraction_log_exam ON extraction_log (exam_paper_id);
