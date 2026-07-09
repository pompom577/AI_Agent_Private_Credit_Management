import React, { useState, useEffect } from 'react';
import { MetricLineage } from '../../pages/TermSheetReview';
import DocumentViewer from './DocumentViewer';
import DocumentSkeleton from './DocumentSkeleton';
import StreamErrorState from './StreamErrorState';

interface SplitScreenProps {
  metric: MetricLineage;
  onClose: () => void;
}

type PdfState = 'loading' | 'loaded' | 'error';

export default function SplitScreenLayout({ metric, onClose }: SplitScreenProps) {
  const [pdfState, setPdfState] = useState<PdfState>('loading');
  // Incrementing this key remounts DocumentViewer on retry, restarting the fetch.
  const [retryKey, setRetryKey] = useState(0);

  // Remount DocumentViewer whenever the analyst selects a different metric row.
  // Incrementing (not resetting to 0) ensures the key always changes, so react-pdf
  // re-fetches even when sourceDocId and pageNumber are the same as the previous metric.
  useEffect(() => {
    setPdfState('loading');
    setRetryKey((k) => k + 1);
  }, [metric.id]);

  const handleRetry = () => {
    setPdfState('loading');
    setRetryKey((k) => k + 1);
  };

  return (
    <div
      style={{
        height: '100%',
        display: 'flex',
        flexDirection: 'column',
        backgroundColor: '#fff',
        borderLeft: '1px solid #dee2e6',
      }}
    >
      {/* Panel header */}
      <div
        style={{
          padding: '16px 24px',
          borderBottom: '1px solid #dee2e6',
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'center',
          backgroundColor: '#fafafa',
          flexShrink: 0,
        }}
      >
        <div>
          <h3 style={{ margin: 0, fontSize: '16px', fontWeight: 'bold' }}>
            Audit Ledger Chain
          </h3>
          <p style={{ margin: '4px 0 0 0', fontSize: '13px', color: '#6c757d' }}>
            Field: <strong>{metric.name}</strong> &rarr; Value:{' '}
            <span style={{ color: '#28a745', fontWeight: 'bold' }}>{metric.value}</span>
          </p>
        </div>
        <button
          onClick={onClose}
          style={{
            border: 'none',
            backgroundColor: 'transparent',
            fontSize: '20px',
            cursor: 'pointer',
            color: '#999',
          }}
          aria-label="Close document panel"
        >
          &times;
        </button>
      </div>

      {/* Document area */}
      <div
        style={{
          flex: 1,
          padding: '24px',
          overflowY: 'auto',
          backgroundColor: '#525659',
          display: 'flex',
          justifyContent: 'center',
          alignItems: 'flex-start',
        }}
      >
        {pdfState === 'loading' && <DocumentSkeleton />}
        {pdfState === 'error' && <StreamErrorState onRetry={handleRetry} />}

        {/* DocumentViewer is mounted while loading so the fetch starts immediately.
            It stays hidden until the PDF is ready, then becomes visible. */}
        <div style={{ display: pdfState === 'loaded' ? 'block' : 'none' }}>
          <DocumentViewer
            key={retryKey}
            sourceDocId={metric.sourceDocId}
            pageNumber={metric.pageNumber}
            bbox={metric.bbox}
            onLoadSuccess={() => setPdfState('loaded')}
            onLoadError={() => setPdfState('error')}
          />
        </div>
      </div>
    </div>
  );
}
