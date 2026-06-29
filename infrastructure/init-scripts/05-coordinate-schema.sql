-- Story 2.0: Spatial bounding box coordinates linked to extracted metrics.
-- ON DELETE RESTRICT prevents destroying historical audit chains.
CREATE TABLE IF NOT EXISTS document_coordinates (
    id          BIGSERIAL PRIMARY KEY,
    metric_id   BIGINT NOT NULL REFERENCES extracted_metrics(id) ON DELETE RESTRICT,
    page_number INT NOT NULL,
    x_min       DOUBLE PRECISION NOT NULL,
    y_min       DOUBLE PRECISION NOT NULL,
    x_max       DOUBLE PRECISION NOT NULL,
    y_max       DOUBLE PRECISION NOT NULL,
    row_index   INT,
    col_index   INT
);
