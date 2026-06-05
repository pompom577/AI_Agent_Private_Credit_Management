-- Create Core Deal Ingestion Tracking Tables (Epic 1)
CREATE TABLE IF NOT EXISTS deals (
    deal_id VARCHAR(255) PRIMARY KEY,
    uploaded_by_user_id VARCHAR(255) NOT NULL,
    bucket_url TEXT NOT NULL,
    status VARCHAR(50) DEFAULT 'INGESTED',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Create Anti-Hallucination Audit Ledger Schema (Epic 2)
CREATE TABLE IF NOT EXISTS metric_audit_log (
    audit_id SERIAL PRIMARY KEY,
    metric_id VARCHAR(255) NOT NULL,
    original_value VARCHAR(255) NOT NULL,
    new_value VARCHAR(255) NOT NULL,
    modified_by_user_id VARCHAR(255) NOT NULL,
    timestamp TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Custom Trigger Function to Block Modifications on the Audit Log Ledger
CREATE OR REPLACE FUNCTION protect_audit_ledger()
RETURNS TRIGGER AS $$
BEGIN
    RAISE EXCEPTION 'Security Policy Violation: Financial audit logs are strictly immutable and append-only.';
END;
$$ LANGUAGE plpgsql;

-- Story 1.2: Per-document classification tracking
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

-- Story 1.3: ensure file_path exists on pre-existing databases.
ALTER TABLE document_records ADD COLUMN IF NOT EXISTS file_path VARCHAR(1000);

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

-- 🎯 FIX: Drop the triggers if they exist so the script never crashes on restart
DROP TRIGGER IF EXISTS block_updates_on_audit_log ON metric_audit_log;
DROP TRIGGER IF EXISTS block_deletes_on_audit_log ON metric_audit_log;

-- Enforce Immutable Protection: Block updates
CREATE TRIGGER block_updates_on_audit_log
BEFORE UPDATE ON metric_audit_log
FOR EACH ROW EXECUTE FUNCTION protect_audit_ledger();

-- Enforce Immutable Protection: Block deletions
CREATE TRIGGER block_deletes_on_audit_log
BEFORE DELETE ON metric_audit_log
FOR EACH ROW EXECUTE FUNCTION protect_audit_ledger();

-- Story 1.3: Extracted metrics from financial PDF tables
CREATE TABLE IF NOT EXISTS extracted_metrics (
    id            BIGSERIAL PRIMARY KEY,
    metric_name   VARCHAR(500) NOT NULL,
    raw_value     VARCHAR(500) NOT NULL,
    unit          VARCHAR(100),
    source_doc_id BIGINT NOT NULL REFERENCES document_records(id) ON DELETE CASCADE,
    page_number   INT,
    created_at    TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);