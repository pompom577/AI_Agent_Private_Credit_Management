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

-- Enforce Immutable Protection: Block updates
CREATE TRIGGER block_updates_on_audit_log
BEFORE UPDATE ON metric_audit_log
FOR EACH ROW EXECUTE FUNCTION protect_audit_ledger();

-- Enforce Immutable Protection: Block deletions
CREATE TRIGGER block_deletes_on_audit_log
BEFORE DELETE ON metric_audit_log
FOR EACH ROW EXECUTE FUNCTION protect_audit_ledger();