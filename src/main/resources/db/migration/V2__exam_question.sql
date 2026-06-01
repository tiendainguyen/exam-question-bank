-- B1: illustrative exam upload + extraction.

CREATE TABLE exam_paper (
    id          UUID         PRIMARY KEY,
    user_id     UUID         NOT NULL,          -- owner (OwnedEntity)
    name        VARCHAR(255) NOT NULL,
    source_type VARCHAR(32)  NOT NULL,          -- ILLUSTRATIVE | GENERATED | BANK
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX idx_exam_paper_user ON exam_paper (user_id);

CREATE TABLE question (
    id               UUID PRIMARY KEY,
    exam_paper_id    UUID NOT NULL REFERENCES exam_paper (id) ON DELETE CASCADE,
    ordinal          INT  NOT NULL,             -- "Câu N"
    stem             TEXT NOT NULL,
    choices          JSONB,                     -- list of options
    correct_answer   VARCHAR(255),
    question_type_id UUID                        -- assigned in B2 (nullable now)
);
CREATE INDEX idx_question_exam ON question (exam_paper_id);
