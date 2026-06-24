// Define the expected JSON data contract coming from the Spring Boot API
export interface ApiLineageResponse {
  metric_id: string;
  metric_name: string;
  value: string;
  source_doc_id: number;
  page_number: number;
  // Bounding box array returned by backend: [x_min, y_min, x_max, y_max]
  bbox: [number, number, number, number];
}

/**
 * Fetches the exact spatial bounding box coordinates and document lineage matching a target metric ID.
 * Target Endpoint: GET /api/v1/metrics/{metric_id}/lineage
 */
export async function fetchMetricLineage(
  metricId: number | string,
): Promise<ApiLineageResponse> {
  const response = await fetch(`/api/v1/metrics/${metricId}/lineage`, {
    method: 'GET',
    headers: {
      Accept: 'application/json',
      'Content-Type': 'application/json',
    },
  });

  if (!response.ok) {
    throw new Error(
      `Lineage API Query Failed: Server returned status code ${response.status}`,
    );
  }

  return response.json();
}
