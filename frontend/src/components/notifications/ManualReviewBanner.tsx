import React from 'react';
import { useNotificationStore } from '../../store/notificationSlice';
import { useExtractionSocket } from '../../hooks/useExtractionSocket';
import { ExtractionToast } from './ExtractionToast';

export const ManualReviewBanner: React.FC = () => {
  // This automatically kicks off the background connection stream loop [cite: 36]
  useExtractionSocket();
  const notifications = useNotificationStore((state) => state.notifications);

  if (notifications.length === 0) return null;

  return (
    <div
      style={{
        position: 'fixed',
        top: '24px',
        right: '24px',
        zIndex: 9999, // Guarantees it hovers cleanly on top of tables and sidebars
        display: 'flex',
        flexDirection: 'column',
        gap: '10px',
        width: '350px',
        pointerEvents: 'none', // TC-FE-03: Allows clicking underlying elements without capturing focus [cite: 52]
      }}
    >
      {notifications.map((notif) => (
        <ExtractionToast
          key={notif.doc_id}
          docId={notif.doc_id}
          filename={notif.filename}
        />
      ))}
    </div>
  );
};
