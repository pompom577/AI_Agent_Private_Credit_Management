import React, { useEffect, useState } from 'react';
import DocumentViewer from '../components/verification/DocumentViewer';
import {
  DocumentRecord,
  ApiLineageResponse,
  fetchDocument,
  fetchDocumentMetrics,
  saveMetricCorrection,
  saveManualMetrics,
} from '../services/lineageApi';

interface ManualRow {
  id: number;
  metric_name: string;
  raw_value: string;
}

export default function OcrReviewPage() {
  const docId = new URLSearchParams(window.location.search).get('doc_id');

  const [doc, setDoc] = useState<DocumentRecord | null>(null);
  const [metrics, setMetrics] = useState<ApiLineageResponse[]>([]);
  const [edits, setEdits] = useState<Record<number, string>>({});
  const [manualRows, setManualRows] = useState<ManualRow[]>([]);
  const [nextRowId, setNextRowId] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [saving, setSaving] = useState(false);
  const [saveMessage, setSaveMessage] = useState<string | null>(null);

  useEffect(() => {
    if (!docId) {
      setError('No document ID in URL. Navigate here via the Deal Ingestion page.');
      setLoading(false);
      return;
    }
    Promise.all([fetchDocument(docId), fetchDocumentMetrics(docId)])
      .then(([docData, metricData]) => {
        setDoc(docData);
        setMetrics(metricData);
        const initialEdits: Record<number, string> = {};
        metricData.forEach((m) => { initialEdits[Number(m.metric_id)] = m.raw_value; });
        setEdits(initialEdits);
      })
      .catch((err) => setError(err.message))
      .finally(() => setLoading(false));
  }, [docId]);

  const addManualRow = () => {
    setManualRows((rows) => [...rows, { id: nextRowId, metric_name: '', raw_value: '' }]);
    setNextRowId((n) => n + 1);
  };

  const updateManualRow = (id: number, field: 'metric_name' | 'raw_value', value: string) => {
    setManualRows((rows) => rows.map((r) => (r.id === id ? { ...r, [field]: value } : r)));
  };

  const removeManualRow = (id: number) => {
    setManualRows((rows) => rows.filter((r) => r.id !== id));
  };

  const handleSave = async () => {
    setSaving(true);
    setSaveMessage(null);
    try {
      // OCR-failed doc: save manually entered rows
      if (metrics.length === 0) {
        const validRows = manualRows.filter((r) => r.metric_name.trim() && r.raw_value.trim());
        if (validRows.length === 0) {
          setSaveMessage('Add at least one metric row before saving.');
          return;
        }
        await saveManualMetrics(docId!, validRows.map(({ metric_name, raw_value }) => ({ metric_name, raw_value })));
        setSaveMessage(`Saved ${validRows.length} metric${validRows.length > 1 ? 's' : ''} successfully.`);
      } else {
        // Existing metrics: save only changed values
        const changed = metrics.filter((m) => edits[Number(m.metric_id)] !== m.raw_value);
        await Promise.all(
          changed.map((m) => saveMetricCorrection(Number(m.metric_id), edits[Number(m.metric_id)])),
        );
        setSaveMessage(
          changed.length === 0
            ? 'No changes to save.'
            : `Saved ${changed.length} correction${changed.length > 1 ? 's' : ''} successfully.`,
        );
      }
    } catch {
      setSaveMessage('Failed to save. Please try again.');
    } finally {
      setSaving(false);
    }
  };

  if (loading) {
    return (
      <div style={{ padding: '40px', textAlign: 'center', color: '#6c757d', fontFamily: 'sans-serif' }}>
        Loading document…
      </div>
    );
  }

  if (error) {
    return (
      <div style={{ padding: '40px', color: '#dc3545', fontFamily: 'sans-serif' }}>
        {error}
      </div>
    );
  }

  return (
    <div style={{ height: 'calc(100vh - 56px)', display: 'flex', flexDirection: 'column', fontFamily: 'sans-serif', backgroundColor: '#f8f9fa' }}>
      {/* Header */}
      <div style={{ padding: '12px 24px', borderBottom: '1px solid #dee2e6', backgroundColor: '#fff', display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexShrink: 0 }}>
        <div>
          <span style={{ fontSize: '18px', fontWeight: 'bold' }}>
            OCR Review — <span style={{ color: '#6c757d' }}>{doc?.filename}</span>
          </span>
          <span style={{
            marginLeft: '12px',
            fontSize: '12px',
            fontWeight: 600,
            padding: '2px 8px',
            borderRadius: '12px',
            backgroundColor: doc?.status === 'Extracted' ? '#d4edda' : '#fff3cd',
            color: doc?.status === 'Extracted' ? '#155724' : '#856404',
          }}>
            {doc?.status}
          </span>
          {doc?.category && (
            <span style={{ marginLeft: '8px', fontSize: '12px', color: '#6c757d' }}>
              {doc.category}
            </span>
          )}
        </div>
        <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
          {saveMessage && (
            <span style={{ fontSize: '13px', color: saveMessage.includes('Failed') ? '#dc3545' : '#28a745' }}>
              {saveMessage}
            </span>
          )}
          <button
            onClick={handleSave}
            disabled={saving}
            style={{
              padding: '8px 18px',
              backgroundColor: saving ? '#6c757d' : '#28a745',
              color: '#fff',
              border: 'none',
              borderRadius: '6px',
              fontWeight: 'bold',
              cursor: saving ? 'not-allowed' : 'pointer',
            }}
          >
            {saving ? 'Saving…' : 'Submit Manual Corrections'}
          </button>
        </div>
      </div>

      {/* Split panels */}
      <div style={{ flex: 1, display: 'grid', gridTemplateColumns: '1fr 1fr', overflow: 'hidden' }}>
        {/* Left — PDF viewer */}
        <div style={{ backgroundColor: '#525659', overflow: 'auto', display: 'flex', justifyContent: 'center', alignItems: 'flex-start', padding: '24px' }}>
          {docId ? (
            <DocumentViewer
              sourceDocId={Number(docId)}
              pageNumber={1}
              bbox={[0, 0, 0, 0]}
            />
          ) : null}
        </div>

        {/* Right — editable metrics table */}
        <div style={{ overflow: 'auto', padding: '24px', borderLeft: '1px solid #dee2e6' }}>
          <h2 style={{ fontSize: '16px', fontWeight: 'bold', margin: '0 0 4px 0', color: '#495057' }}>
            Extracted Metrics
          </h2>
          <p style={{ fontSize: '13px', color: '#6c757d', margin: '0 0 16px 0' }}>
            Edit any value below and click Submit to save corrections to the audit ledger.
          </p>

          {metrics.length === 0 ? (
            <div>
              <div style={{ padding: '12px 16px', backgroundColor: '#fff3cd', border: '1px solid #ffeeba', borderRadius: '6px', marginBottom: '16px', fontSize: '13px', color: '#856404' }}>
                Automatic extraction failed for this document. Read the PDF on the left and enter the values manually below.
              </div>
              <table style={{ width: '100%', borderCollapse: 'collapse', backgroundColor: '#fff', borderRadius: '8px', overflow: 'hidden', boxShadow: '0 1px 4px rgba(0,0,0,0.06)', marginBottom: '12px' }}>
                <thead>
                  <tr style={{ backgroundColor: '#e9ecef', borderBottom: '2px solid #dee2e6' }}>
                    <th style={{ padding: '10px 12px', fontSize: '13px', textAlign: 'left', width: '50%' }}>Metric Name</th>
                    <th style={{ padding: '10px 12px', fontSize: '13px', textAlign: 'left', width: '40%' }}>Value</th>
                    <th style={{ padding: '10px 12px', width: '10%' }} />
                  </tr>
                </thead>
                <tbody>
                  {manualRows.map((row) => (
                    <tr key={row.id} style={{ borderBottom: '1px solid #dee2e6' }}>
                      <td style={{ padding: '6px 12px' }}>
                        <input
                          placeholder="e.g. Total Assets"
                          value={row.metric_name}
                          onChange={(e) => updateManualRow(row.id, 'metric_name', e.target.value)}
                          style={{ width: '100%', padding: '4px 8px', fontSize: '13px', border: '1px solid #ced4da', borderRadius: '4px', boxSizing: 'border-box' }}
                        />
                      </td>
                      <td style={{ padding: '6px 12px' }}>
                        <input
                          placeholder="e.g. 19,640,164"
                          value={row.raw_value}
                          onChange={(e) => updateManualRow(row.id, 'raw_value', e.target.value)}
                          style={{ width: '100%', padding: '4px 8px', fontSize: '13px', border: '1px solid #ced4da', borderRadius: '4px', fontFamily: 'monospace', boxSizing: 'border-box' }}
                        />
                      </td>
                      <td style={{ padding: '6px 8px', textAlign: 'center' }}>
                        <button
                          onClick={() => removeManualRow(row.id)}
                          style={{ background: 'none', border: 'none', cursor: 'pointer', color: '#dc3545', fontSize: '16px', padding: '2px 6px' }}
                          aria-label="Remove row"
                        >
                          ×
                        </button>
                      </td>
                    </tr>
                  ))}
                  {manualRows.length === 0 && (
                    <tr>
                      <td colSpan={3} style={{ padding: '16px', textAlign: 'center', color: '#adb5bd', fontSize: '13px' }}>
                        Click "Add Row" to start entering metrics.
                      </td>
                    </tr>
                  )}
                </tbody>
              </table>
              <button
                onClick={addManualRow}
                style={{ padding: '6px 14px', backgroundColor: '#007bff', color: '#fff', border: 'none', borderRadius: '4px', cursor: 'pointer', fontSize: '13px', fontWeight: 600 }}
              >
                + Add Row
              </button>
            </div>
          ) : (
            <table style={{ width: '100%', borderCollapse: 'collapse', backgroundColor: '#fff', borderRadius: '8px', overflow: 'hidden', boxShadow: '0 1px 4px rgba(0,0,0,0.06)' }}>
              <thead>
                <tr style={{ backgroundColor: '#e9ecef', textAlign: 'left', borderBottom: '2px solid #dee2e6' }}>
                  <th style={{ padding: '10px 12px', fontSize: '13px' }}>Metric</th>
                  <th style={{ padding: '10px 12px', fontSize: '13px' }}>Value</th>
                </tr>
              </thead>
              <tbody>
                {metrics.map((m) => {
                  const id = Number(m.metric_id);
                  const changed = edits[id] !== m.raw_value;
                  return (
                    <tr key={id} style={{ borderBottom: '1px solid #dee2e6', backgroundColor: changed ? '#fff8e1' : 'transparent' }}>
                      <td style={{ padding: '8px 12px', fontSize: '13px', fontWeight: 500, width: '55%' }}>
                        {m.metric_name}
                      </td>
                      <td style={{ padding: '6px 12px', width: '45%' }}>
                        <input
                          value={edits[id] ?? ''}
                          onChange={(e) => setEdits((prev) => ({ ...prev, [id]: e.target.value }))}
                          style={{
                            width: '100%',
                            padding: '4px 8px',
                            fontSize: '13px',
                            border: `1px solid ${changed ? '#f0ad4e' : '#ced4da'}`,
                            borderRadius: '4px',
                            fontFamily: 'monospace',
                            boxSizing: 'border-box',
                            backgroundColor: changed ? '#fffdf0' : '#fff',
                          }}
                        />
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          )}
        </div>
      </div>
    </div>
  );
}
