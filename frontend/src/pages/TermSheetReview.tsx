import React, { useState, useEffect } from 'react';
import SplitScreenLayout from '../components/verification/SplitScreenLayout';
import { fetchAllMetrics, ApiLineageResponse } from '../services/lineageApi';

export interface MetricLineage {
  id: number;
  name: string;
  value: string;
  pageNumber: number;
  // null when using demo fallback data (backend offline); backend stream URL skipped.
  sourceDocId: number | null;
  bbox: [number, number, number, number];
}

const FALLBACK_METRICS: MetricLineage[] = [
  {
    id: 1,
    name: 'Total Revenue',
    value: '$12,500,000',
    pageNumber: 1,
    sourceDocId: null,
    bbox: [20, 25, 45, 29],
  },
  {
    id: 2,
    name: 'Net EBITDA',
    value: '$4,700,000',
    pageNumber: 1,
    sourceDocId: null,
    bbox: [20, 42, 45, 46],
  },
  {
    id: 3,
    name: 'Total Liabilities',
    value: '$1,250,000',
    pageNumber: 2,
    sourceDocId: null,
    bbox: [55, 72, 85, 76],
  },
];

function toMetricLineage(a: ApiLineageResponse): MetricLineage {
  return {
    id: Number(a.metric_id),
    name: a.metric_name,
    value: a.raw_value,
    pageNumber: a.page_number,
    sourceDocId: a.source_doc_id,
    bbox: a.bbox,
  };
}

export default function TermSheetReview() {
  const [metrics, setMetrics] = useState<MetricLineage[]>(FALLBACK_METRICS);
  const [selectedMetric, setSelectedMetric] = useState<MetricLineage | null>(null);
  const [apiError, setApiError] = useState<string | null>(null);
  const [metricsLoading, setMetricsLoading] = useState(true);

  // Load real metrics from the DB on mount. Falls back to demo data if offline or empty.
  useEffect(() => {
    fetchAllMetrics()
      .then((data) => {
        if (data.length > 0) {
          setMetrics(data.map(toMetricLineage));
          setApiError(null);
        }
      })
      .catch(() => {
        setApiError('Backend disconnected. Displaying demo metrics.');
      })
      .finally(() => setMetricsLoading(false));
  }, []);

  // All data is already loaded via fetchAllMetrics — just open the panel directly.
  // A second per-click API call is not needed and would 404 when coordinates are missing.
  const handleVerifySource = (metric: MetricLineage) => {
    setSelectedMetric(metric);
    setApiError(null);
  };

  // NavBar is 56px (h-14). Subtract so the flex layout doesn't overflow the viewport.
  const PAGE_HEIGHT = 'calc(100vh - 56px)';

  return (
    <div
      style={{
        display: 'flex',
        height: PAGE_HEIGHT,
        fontFamily: 'sans-serif',
        backgroundColor: '#f8f9fa',
      }}
    >
      {/* Left panel — metric table */}
      <div
        style={{
          flex: 1,
          padding: '24px',
          overflowY: 'auto',
          borderRight: '2px solid #dee2e6',
        }}
      >
        <h1 style={{ fontSize: '24px', fontWeight: 'bold', marginBottom: '4px' }}>
          Extracted Term Sheet Spreads
        </h1>
        <p style={{ color: '#6c757d', fontSize: '14px', marginBottom: '24px' }}>
          Click a financial metric cell below to audit its spatial lineage ledger tracking data.
        </p>

        {apiError && (
          <div
            style={{
              padding: '10px 14px',
              backgroundColor: '#fff3cd',
              color: '#856404',
              borderRadius: '6px',
              marginBottom: '16px',
              fontSize: '13px',
              border: '1px solid #ffeeba',
            }}
          >
            {apiError}
          </div>
        )}

        {metricsLoading && (
          <p style={{ color: '#6c757d', fontSize: '13px', marginBottom: '12px' }}>
            Loading metrics from database…
          </p>
        )}

        <table
          style={{
            width: '100%',
            borderCollapse: 'collapse',
            backgroundColor: '#fff',
            borderRadius: '8px',
            overflow: 'hidden',
            boxShadow: '0 2px 4px rgba(0,0,0,0.05)',
          }}
        >
          <thead>
            <tr
              style={{
                backgroundColor: '#e9ecef',
                textAlign: 'left',
                borderBottom: '2px solid #dee2e6',
              }}
            >
              <th style={{ padding: '12px' }}>Metric Field</th>
              <th style={{ padding: '12px' }}>Extracted Value</th>
              <th style={{ padding: '12px' }}>Action</th>
            </tr>
          </thead>
          <tbody>
            {metrics.map((metric) => (
              <tr
                key={metric.id}
                style={{
                  borderBottom: '1px solid #dee2e6',
                  backgroundColor:
                    selectedMetric?.id === metric.id ? '#e7f1ff' : 'transparent',
                  transition: 'background-color 0.2s ease',
                }}
              >
                <td style={{ padding: '12px', fontWeight: 600 }}>{metric.name}</td>
                <td style={{ padding: '12px', color: '#28a745', fontWeight: 'bold' }}>
                  {metric.value}
                </td>
                <td style={{ padding: '12px' }}>
                  <button
                    onClick={() => handleVerifySource(metric)}
                    style={{
                      padding: '6px 12px',
                      backgroundColor:
                        selectedMetric?.id === metric.id ? '#0056b3' : '#007bff',
                      color: '#fff',
                      border: 'none',
                      borderRadius: '4px',
                      cursor: 'pointer',
                      fontWeight: 600,
                      transition: 'background-color 0.2s ease',
                    }}
                  >
                    Verify Source →
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      {/* Right panel — PDF viewer */}
      <div
        style={{
          flex: selectedMetric ? 1 : 0,
          transition: 'all 0.3s cubic-bezier(0.4, 0, 0.2, 1)',
          display: 'flex',
          flexDirection: 'column',
          overflow: 'hidden',
          boxShadow: selectedMetric ? '-4px 0 16px rgba(0,0,0,0.05)' : 'none',
        }}
      >
        {selectedMetric ? (
          <SplitScreenLayout
            metric={selectedMetric}
            onClose={() => setSelectedMetric(null)}
          />
        ) : (
          <div
            style={{
              flex: 1,
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              color: '#6c757d',
              padding: '20px',
              textAlign: 'center',
            }}
          >
            <div style={{ maxWidth: '280px' }}>
              <svg
                xmlns="http://www.w3.org/2000/svg"
                style={{ width: '48px', height: '48px', color: '#ced4da', marginBottom: '12px' }}
                fill="none"
                viewBox="0 0 24 24"
                stroke="currentColor"
              >
                <path
                  strokeLinecap="round"
                  strokeLinejoin="round"
                  strokeWidth={1.5}
                  d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2m-3 7h3m-3 4h3m-6-4h.01M9 16h.01"
                />
              </svg>
              <p style={{ margin: 0, fontSize: '14px', fontWeight: 500 }}>
                Select a metrics data point cell row to open side-by-side file validation canvas panels.
              </p>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}
