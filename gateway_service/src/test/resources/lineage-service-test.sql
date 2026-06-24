-- H2-compatible schema for LineageServiceTest.
-- No cross-table FKs so tests can insert metrics/coordinates independently.
CREATE TABLE IF NOT EXISTS extracted_metrics (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    metric_name   VARCHAR(500) NOT NULL,
    raw_value     VARCHAR(500) NOT NULL,
    unit          VARCHAR(100),
    source_doc_id BIGINT NOT NULL,
    page_number   INT
);

CREATE TABLE IF NOT EXISTS document_coordinates (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    metric_id   BIGINT NOT NULL,
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
