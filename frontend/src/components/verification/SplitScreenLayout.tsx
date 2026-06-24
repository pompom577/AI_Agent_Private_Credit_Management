import React, { useState, useEffect } from 'react';
import { MetricLineage } from '../../pages/TermSheetReview';
import DocumentViewer from './DocumentViewer'; // 🌟 Import your new viewer component!

interface SplitScreenProps {
  metric: MetricLineage;
  onClose: () => void;
}

export default function SplitScreenLayout({
  metric,
  onClose,
}: SplitScreenProps) {
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    setIsLoading(true);
    const timer = setTimeout(() => {
      setIsLoading(false);
    }, 600);
    return () => clearTimeout(timer);
  }, [metric]);

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
      <div
        style={{
          padding: '16px 24px',
          borderBottom: '1px solid #dee2e6',
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'center',
          backgroundColor: '#fafafa',
        }}
      >
        <div>
          <h3 style={{ margin: 0, fontSize: '16px', fontWeight: 'bold' }}>
            🔍 Audit Ledger Chain
          </h3>
          <p
            style={{ margin: '4px 0 0 0', fontSize: '13px', color: '#6c757d' }}
          >
            Field: <strong>{metric.name}</strong> ➜ Value:{' '}
            <span style={{ color: '#28a745', fontWeight: 'bold' }}>
              {metric.value}
            </span>
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
        >
          ×
        </button>
      </div>

      <div
        style={{
          flex: 1,
          padding: '24px',
          overflowY: 'auto',
          backgroundColor: '#525659',
          display: 'flex',
          justifyContent: 'center',
        }}
      >
        {isLoading ? (
          <div
            style={{
              width: '450px',
              height: '600px',
              backgroundColor: '#fff',
              borderRadius: '4px',
              padding: '20px',
              display: 'flex',
              flexDirection: 'column',
              gap: '15px',
            }}
          >
            <div
              style={{
                height: '24px',
                backgroundColor: '#e9ecef',
                borderRadius: '4px',
                width: '50%',
              }}
            />
            <div
              style={{
                height: '480px',
                backgroundColor: '#e9ecef',
                borderRadius: '4px',
              }}
            />
          </div>
        ) : (
          <DocumentViewer pageNumber={metric.pageNumber} bbox={metric.bbox} />
        )}
      </div>
    </div>
  );
}
