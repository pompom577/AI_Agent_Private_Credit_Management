import React from 'react';
import { useNotificationStore } from '../../store/notificationSlice';

interface ExtractionToastProps {
  docId: number;
  filename: string;
}

export const ExtractionToast: React.FC<ExtractionToastProps> = ({
  docId,
  filename,
}) => {
  const dismissNotification = useNotificationStore(
    (state) => state.dismissNotification,
  );

  const handleActionRouting = () => {
    // TC-FE-04: Direct deep-linking route to your manual review panel [cite: 52]
    console.log(
      `Navigating analyst to manual review screen for document: ${docId}`,
    );
    window.location.href = `/ocr-review?doc_id=${docId}`;
    dismissNotification(docId);
  };

  return (
    <div
      style={{
        background: '#fff3cd',
        color: '#856404',
        border: '1px solid #ffeeba',
        padding: '16px',
        borderRadius: '6px',
        boxShadow: '0 4px 12px rgba(0,0,0,0.12)',
        display: 'flex',
        justifyContent: 'space-between',
        alignItems: 'center',
        gap: '16px',
        pointerEvents: 'auto', // Allows interacting with the banner elements smoothly [cite: 52]
        transition: 'all 0.3s ease',
      }}
    >
      <div
        style={{ cursor: 'pointer', flexGrow: 1 }}
        onClick={handleActionRouting}
      >
        <strong style={{ display: 'block', marginBottom: '2px' }}>
          ⚠️ OCR Review Required
        </strong>
        {/* TC-FE-02: Human-readable error message banner output configuration [cite: 52] */}
        <span style={{ fontSize: '13px' }}>
          OCR review required for {filename}
        </span>
      </div>
      <button
        onClick={() => dismissNotification(docId)}
        style={{
          background: 'transparent',
          border: 'none',
          color: '#856404',
          fontWeight: 'bold',
          cursor: 'pointer',
          fontSize: '18px',
        }}
      >
        &times;
      </button>
    </div>
  );
};
