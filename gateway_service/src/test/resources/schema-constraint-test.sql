-- Minimal H2-compatible schema for TC-GW-01 / TC-GW-02 schema constraint tests.
-- Uses BIGINT AUTO_INCREMENT (not BIGSERIAL) so H2 can parse it without PostgreSQL mode.
CREATE TABLE IF NOT EXISTS deals (
    deal_id             VARCHAR(255) PRIMARY KEY,
    uploaded_by_user_id VARCHAR(255) NOT NULL,
    bucket_url          TEXT         NOT NULL,
    status              VARCHAR(50)  DEFAULT 'INGESTED'
);

CREATE TABLE IF NOT EXISTS document_records (
    id       BIGINT AUTO_INCREMENT PRIMARY KEY,
    deal_id  VARCHAR(255) NOT NULL REFERENCES deals(deal_id) ON DELETE CASCADE,
    filename VARCHAR(500) NOT NULL,
    status   VARCHAR(50)  NOT NULL DEFAULT 'PENDING',
    CONSTRAINT uq_doc_deal_file UNIQUE (deal_id, filename)
);

CREATE TABLE IF NOT EXISTS extracted_metrics (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    metric_name   VARCHAR(500) NOT NULL,
    raw_value     VARCHAR(500) NOT NULL,
    unit          VARCHAR(100),
    source_doc_id BIGINT NOT NULL REFERENCES document_records(id) ON DELETE CASCADE,
    page_number   INT
);

CREATE TABLE IF NOT EXISTS document_coordinates (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    metric_id   BIGINT NOT NULL REFERENCES extracted_metrics(id) ON DELETE RESTRICT,
    page_number INT NOT NULL,
    x_min       DOUBLE NOT NULL,
    y_min       DOUBLE NOT NULL,
    x_max       DOUBLE NOT NULL,
    y_max       DOUBLE NOT NULL,
    row_index   INT,
    col_index   INT
);

CREATE TABLE IF NOT EXISTS metric_audit_log (
    audit_id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    metric_id            VARCHAR(255) NOT NULL,
    original_value       VARCHAR(255) NOT NULL,
    new_value            VARCHAR(255) NOT NULL,
    modified_by_user_id  VARCHAR(255) NOT NULL,
    timestamp            TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS quarantine (
    quarantine_id UUID PRIMARY KEY,
    endpoint      VARCHAR(500)  NOT NULL,
    payload       VARCHAR(4000) NOT NULL,
    agent_id      VARCHAR(255),
    status        VARCHAR(50)   NOT NULL DEFAULT 'Pending'
);

CREATE TABLE IF NOT EXISTS hitl_audit_ledger (
    audit_id      BIGINT AUTO_INCREMENT PRIMARY KEY,
    action_url    VARCHAR(500) NOT NULL,
    timestamp     TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    decision      VARCHAR(20)  NOT NULL,
    officer_id    UUID NOT NULL,
    quarantine_id UUID NOT NULL REFERENCES quarantine(quarantine_id) ON DELETE RESTRICT
);
