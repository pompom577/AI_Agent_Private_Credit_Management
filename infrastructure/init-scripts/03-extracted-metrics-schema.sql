-- Story 1.3 Migration: Extracted metrics table for tabular data from financial PDFs
CREATE TABLE IF NOT EXISTS extracted_metrics (
    id            BIGSERIAL PRIMARY KEY,
    metric_name   VARCHAR(500) NOT NULL,
    raw_value     VARCHAR(500) NOT NULL,
    unit          VARCHAR(100),
    source_doc_id BIGINT NOT NULL REFERENCES document_records(id) ON DELETE CASCADE,
    page_number   INT,
    created_at    TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);
