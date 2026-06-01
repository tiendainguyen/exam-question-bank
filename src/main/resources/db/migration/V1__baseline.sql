-- Baseline schema for exam-question-bank.
-- pgvector is enabled here so later epics (B2 question_type.centroid,
-- B4 bank_question.embedding) can add vector columns without another extension step.
CREATE EXTENSION IF NOT EXISTS vector;

CREATE TABLE app_user (
    id            UUID         PRIMARY KEY,
    email         VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    display_name  VARCHAR(255),
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now()
);
