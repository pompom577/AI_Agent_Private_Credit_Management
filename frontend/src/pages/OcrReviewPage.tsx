import React, { useEffect, useState } from 'react';

export default function OcrReviewPage() {
  // 1. Parse the document ID out of the URL string parameters
  const queryParams = new URLSearchParams(window.location.search);
  const docId = queryParams.get('doc_id') || 'Unknown';

  // State management for mock document properties
  const [docDetails, setDocDetails] = useState({
    filename: 'InvestCo_Report.pdf',
    status: 'Requires Manual Review',
    confidenceScore: '42%',
    extractedText:
      'TOTAL REVENUE [OCR ERROR: SMUDGED] ... $1,250,000\nNET INCOME ... [ILLEGIBLE]',
  });

  const [correctedText, setCorrectedText] = useState(docDetails.extractedText);

  const handleSaveCorrection = () => {
    // This is where you would perform an API PUT/POST call to your backend database
    console.log(
      `Saving updated manual review metrics text for document ${docId}:`,
      correctedText,
    );
    alert(
      `Document ID ${docId} changes saved successfully! Returning to dashboard.`,
    );
    window.location.href = '/';
  };

  return (
    <div
      className="min-h-screen"
      style={{
        backgroundColor: '#f8f9fa',
        fontFamily: 'sans-serif',
        padding: '24px',
      }}
    >
      {/* Upper Navigation Bar Section */}
      <header
        style={{
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'center',
          marginBottom: '24px',
          borderBottom: '1px solid #e9ecef',
          paddingBottom: '16px',
        }}
      >
        <div>
          <button
            onClick={() => (window.location.href = '/')}
            style={{
              padding: '8px 12px',
              border: '1px solid #ced4da',
              borderRadius: '4px',
              backgroundColor: '#fff',
              cursor: 'pointer',
              marginRight: '12px',
            }}
          >
            ← Back to Dashboard
          </button>
          <span style={{ fontSize: '20px', fontWeight: 'bold' }}>
            Document Workspace:{' '}
            <span style={{ color: '#6c757d' }}>#{docId}</span>
          </span>
        </div>
        <button
          onClick={handleSaveCorrection}
          style={{
            padding: '10px 20px',
            backgroundColor: '#28a745',
            color: '#fff',
            border: 'none',
            borderRadius: '6px',
            fontWeight: 'bold',
            cursor: 'pointer',
          }}
        >
          ✅ Submit Manual Corrections
        </button>
      </header>

      {/* Main Split-Screen Dashboard Panel Layout Layout Container */}
      <div
        style={{
          display: 'grid',
          gridTemplateColumns: '1fr 1fr',
          gap: '24px',
          height: 'calc(100vh - 120px)',
        }}
      >
        {/* LEFT SIDE PANEL: File Display Stream Simulation */}
        <div
          style={{
            backgroundColor: '#fff',
            border: '1px solid #dee2e6',
            borderRadius: '8px',
            padding: '20px',
            display: 'flex',
            flexDirection: 'column',
          }}
        >
          <h2
            style={{
              fontSize: '16px',
              fontWeight: 'bold',
              marginTop: 0,
              marginBottom: '12px',
              color: '#495057',
            }}
          >
            📁 Original File View:{' '}
            <span style={{ color: '#007bff' }}>{docDetails.filename}</span>
          </h2>
          <div
            style={{
              flexGrow: 1,
              backgroundColor: '#e9ecef',
              borderRadius: '6px',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              border: '2px dashed #ced4da',
            }}
          >
            <div style={{ textAlign: 'center', color: '#6c757d' }}>
              <svg
                xmlns="http://www.w3.org/2000/svg"
                style={{ width: '48px', height: '48px', marginBottom: '12px' }}
                fill="none"
                viewBox="0 0 24 24"
                stroke="currentColor"
              >
                <path
                  strokeLinecap="round"
                  strokeLinejoin="round"
                  strokeWidth={2}
                  d="M7 21h10a2 2 0 002-2V9.414a1 1 0 00-.293-.707l-5.414-5.414A1 1 0 0012.586 3H7a2 2 0 00-2 2v14a2 2 0 002 2z"
                />
              </svg>
              <p style={{ fontWeight: 'bold' }}>PDF Canvas Rendering Panel</p>
              <p style={{ fontSize: '13px' }}>
                Simulating secure storage bucket object viewer pipeline
              </p>
            </div>
          </div>
        </div>

        {/* RIGHT SIDE PANEL: Interactive Editing Field Correction Engine Forms */}
        <div
          style={{
            backgroundColor: '#fff',
            border: '1px solid #dee2e6',
            borderRadius: '8px',
            padding: '20px',
            display: 'flex',
            flexDirection: 'column',
          }}
        >
          <h2
            style={{
              fontSize: '16px',
              fontWeight: 'bold',
              marginTop: 0,
              marginBottom: '4px',
              color: '#495057',
            }}
          >
            📝 Extracted Telemetry Review
          </h2>
          <p
            style={{
              fontSize: '13px',
              color: '#dc3545',
              marginTop: 0,
              marginBottom: '16px',
            }}
          >
            Low AI Confidence Threshold Match:{' '}
            <strong>{docDetails.confidenceScore}</strong>
          </p>

          <label
            style={{
              display: 'block',
              fontWeight: 'bold',
              fontSize: '14px',
              marginBottom: '8px',
              color: '#495057',
            }}
          >
            Editable Raw Text Stream Content:
          </label>
          <textarea
            value={correctedText}
            onChange={(e) => setCorrectedText(e.target.value)}
            style={{
              flexGrow: 1,
              width: '100%',
              padding: '12px',
              fontFamily: 'monospace',
              fontSize: '14px',
              border: '1px solid #ced4da',
              borderRadius: '6px',
              resize: 'none',
              boxSizing: 'border-box',
              lineHeight: '1.5',
              backgroundColor: '#fffdf5',
            }}
          />
        </div>
      </div>
    </div>
  );
}
