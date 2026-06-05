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
