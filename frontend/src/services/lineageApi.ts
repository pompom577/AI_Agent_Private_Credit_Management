// Matches LineageResponse record from the Spring Boot gateway (raw_value, not value).
export interface ApiLineageResponse {
  metric_id: string;
  metric_name: string;
  raw_value: string;
  source_doc_id: number;
  page_number: number;
  bbox: [number, number, number, number];
}

/**
 * Fetches all extracted metrics with their spatial coordinates.
 * Used to populate the Term Sheet Review table after a deal is ingested.
 * GET /api/v1/metrics
 */
export async function fetchAllMetrics(): Promise<ApiLineageResponse[]> {
  const response = await fetch('/api/v1/metrics', {
    headers: { Accept: 'application/json' },
  });
  if (!response.ok) {
    throw new Error(`Failed to load metrics: ${response.status}`);
  }
  return response.json();
}

/**
 * Fetches the lineage for a single metric by ID.
 * GET /api/v1/metrics/{metricId}/lineage
 */
export async function fetchMetricLineage(
  metricId: number | string,
): Promise<ApiLineageResponse> {
  const response = await fetch(`/api/v1/metrics/${metricId}/lineage`, {
    headers: { Accept: 'application/json' },
  });
  if (!response.ok) {
    throw new Error(`Lineage API Query Failed: Server returned status code ${response.status}`);
  }
  return response.json();
}

export interface DocumentRecord {
  id: number;
  filename: string;
  category: string | null;
  status: string;
  page_count: number | null;
}

/** GET /api/v1/documents/{id} — document metadata for the OCR review page. */
export async function fetchDocument(docId: number | string): Promise<DocumentRecord> {
  const response = await fetch(`/api/v1/documents/${docId}`, {
    headers: { Accept: 'application/json' },
  });
  if (!response.ok) throw new Error(`Document not found: ${response.status}`);
  return response.json();
}

/** GET /api/v1/documents/{id}/metrics — all extracted metrics for one document. */
export async function fetchDocumentMetrics(docId: number | string): Promise<ApiLineageResponse[]> {
  const response = await fetch(`/api/v1/documents/${docId}/metrics`, {
    headers: { Accept: 'application/json' },
  });
  if (!response.ok) throw new Error(`Failed to load metrics: ${response.status}`);
  return response.json();
}

/** PUT /api/v1/metrics/{id} — save a manual correction for one metric. */
export async function saveMetricCorrection(
  metricId: number,
  newValue: string,
): Promise<void> {
  const response = await fetch(`/api/v1/metrics/${metricId}`, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ new_value: newValue, user_id: 'ocr-reviewer' }),
  });
  if (!response.ok) throw new Error(`Save failed: ${response.status}`);
}

/** POST /api/v1/documents/{id}/metrics — save manually entered metrics for an OCR-failed doc. */
export async function saveManualMetrics(
  docId: number | string,
  entries: { metric_name: string; raw_value: string }[],
): Promise<void> {
  const response = await fetch(`/api/v1/documents/${docId}/metrics`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(entries),
  });
  if (!response.ok) throw new Error(`Save failed: ${response.status}`);
}
