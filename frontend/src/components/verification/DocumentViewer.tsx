import React, { useState } from 'react';
import { Document, Page, pdfjs } from 'react-pdf';
import CoordinateOverlay from './CoordinateOverlay';
import 'react-pdf/dist/Page/AnnotationLayer.css';
import 'react-pdf/dist/Page/TextLayer.css';

// 🛠️ REQUIRED: Link react-pdf to the standard CDNs worker script so it renders fast in the background
pdfjs.GlobalWorkerOptions.workerSrc = `//unpkg.com/pdfjs-dist@${pdfjs.version}/build/pdf.worker.min.mjs`;

interface DocumentViewerProps {
  pageNumber: number;
  bbox: [number, number, number, number];
}

export default function DocumentViewer({
  pageNumber,
  bbox,
}: DocumentViewerProps) {
  const [numPages, setNumPages] = useState<number | null>(null);

  // Fallback demo file: A public sample financial/text PDF hosted securely online
  const samplePdfUrl =
    'https://raw.githubusercontent.com/mozilla/pdf.js/master/web/compressed.tracemonkey-pldi-09.pdf';

  function onDocumentLoadSuccess({ numPages }: { numPages: number }) {
    setNumPages(numPages);
  }

  return (
    <div
      style={{
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
        width: '100%',
        color: '#fff',
      }}
    >
      <Document
        file={samplePdfUrl}
        onLoadSuccess={onDocumentLoadSuccess}
        loading={
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
                width: '40%',
              }}
            />
            <div
              style={{
                height: '500px',
                backgroundColor: '#e9ecef',
                borderRadius: '4px',
              }}
            />
          </div>
        }
        error={
          <div
            style={{
              padding: '20px',
              color: '#dc3545',
              backgroundColor: '#fff',
              borderRadius: '4px',
            }}
          >
            Failed to load source PDF document.
          </div>
        }
      >
        <div
          id="pdf-page-render-viewport"
          style={{
            position: 'relative',
            boxShadow: '0 4px 12px rgba(0,0,0,0.3)',
            borderRadius: '4px',
          }}
        >
          <Page
            pageNumber={pageNumber || 1}
            width={450}
            renderAnnotationLayer={false}
            renderTextLayer={true}
          />

          <CoordinateOverlay bbox={bbox} pageNumber={pageNumber} />
        </div>
      </Document>

      {numPages && (
        <p style={{ marginTop: '12px', fontSize: '13px', color: '#ccc' }}>
          Document Page {pageNumber} of {numPages}
        </p>
      )}
    </div>
  );
}
