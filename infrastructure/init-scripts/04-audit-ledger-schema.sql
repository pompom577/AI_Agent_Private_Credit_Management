-- Story 2.0: Append-only audit ledger for human-in-the-loop metric overwrites.
CREATE TABLE IF NOT EXISTS metric_audit_log (
    audit_id             BIGSERIAL PRIMARY KEY,
    metric_id            VARCHAR(255) NOT NULL,
    original_value       VARCHAR(255) NOT NULL,
    new_value            VARCHAR(255) NOT NULL,
    modified_by_user_id  VARCHAR(255) NOT NULL,
    timestamp            TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE OR REPLACE FUNCTION protect_audit_ledger()
RETURNS TRIGGER AS $$
BEGIN
    RAISE EXCEPTION 'Security Policy Violation: Financial audit logs are strictly immutable and append-only.';
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS block_updates_on_audit_log ON metric_audit_log;
DROP TRIGGER IF EXISTS block_deletes_on_audit_log ON metric_audit_log;

CREATE TRIGGER block_updates_on_audit_log
BEFORE UPDATE ON metric_audit_log
FOR EACH ROW EXECUTE FUNCTION protect_audit_ledger();

CREATE TRIGGER block_deletes_on_audit_log
BEFORE DELETE ON metric_audit_log
FOR EACH ROW EXECUTE FUNCTION protect_audit_ledger();
