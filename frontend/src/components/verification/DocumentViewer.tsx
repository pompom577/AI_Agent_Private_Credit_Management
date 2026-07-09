import React, { useState } from 'react';
import { Document, Page, pdfjs } from 'react-pdf';
import CoordinateOverlay from './CoordinateOverlay';
import 'react-pdf/dist/Page/AnnotationLayer.css';
import 'react-pdf/dist/Page/TextLayer.css';

pdfjs.GlobalWorkerOptions.workerSrc = `//unpkg.com/pdfjs-dist@${pdfjs.version}/build/pdf.worker.min.mjs`;

interface DocumentViewerProps {
  pageNumber: number;
  bbox: [number, number, number, number];
  sourceDocId?: number | null;
  onLoadSuccess?: () => void;
  onLoadError?: () => void;
}

// Fallback used when sourceDocId is absent (demo / backend offline).
const DEMO_PDF_URL =
  'https://raw.githubusercontent.com/mozilla/pdf.js/master/web/compressed.tracemonkey-pldi-09.pdf';

export default function DocumentViewer({
  pageNumber,
  bbox,
  sourceDocId,
  onLoadSuccess,
  onLoadError,
}: DocumentViewerProps) {
  const [numPages, setNumPages] = useState<number | null>(null);

  const pdfUrl =
    sourceDocId != null
      ? `/documents/${sourceDocId}/page/${pageNumber}`
      : DEMO_PDF_URL;

  function handleLoadSuccess({ numPages: n }: { numPages: number }) {
    setNumPages(n);
    onLoadSuccess?.();
  }

  function handleLoadError() {
    onLoadError?.();
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
        file={pdfUrl}
        onLoadSuccess={handleLoadSuccess}
        onLoadError={handleLoadError}
        loading={null}
        error={null}
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
