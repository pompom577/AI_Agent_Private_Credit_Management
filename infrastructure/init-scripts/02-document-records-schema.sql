-- Story 1.2 Migration: Per-document classification tracking table
CREATE TABLE IF NOT EXISTS document_records (
    id              BIGSERIAL PRIMARY KEY,
    deal_id         VARCHAR(255) NOT NULL
                        REFERENCES deals(deal_id) ON DELETE CASCADE,
    filename        VARCHAR(500) NOT NULL,
    category        VARCHAR(100),
    status          VARCHAR(50)  NOT NULL DEFAULT 'PENDING',
    file_path       VARCHAR(1000),
    created_at      TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_document_records_deal_filename UNIQUE (deal_id, filename)
);

-- Story 1.3: ensure file_path exists on databases created before this column
-- was introduced (CREATE TABLE IF NOT EXISTS above is a no-op on those).
ALTER TABLE document_records ADD COLUMN IF NOT EXISTS file_path VARCHAR(1000);

-- Auto-update updated_at on every row change
CREATE OR REPLACE FUNCTION set_document_record_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_document_records_updated_at ON document_records;
CREATE TRIGGER trg_document_records_updated_at
BEFORE UPDATE ON document_records
FOR EACH ROW EXECUTE FUNCTION set_document_record_updated_at();
