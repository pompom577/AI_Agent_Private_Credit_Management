import { useEffect, useRef } from 'react';
import {
  useNotificationStore,
  ExtractionNotification,
} from '../store/notificationSlice';

// Must match async_pipeline.STATUS_EXTRACTION_FAILED_REQUIRES_OCR (FastAPI)
// and the value forwarded by the gateway's NotificationBridge.
const REQUIRES_OCR_STATUS = 'Extraction_Failed_Requires_OCR';

/**
 * Subscribes to the Spring Boot gateway's per-user SSE stream
 * (GET /sse/extraction-updates?userId=...). The gateway pushes named
 * "extraction-failure" events that it routes only to the analyst who owns the
 * deal (NotificationBridge / TC-GW-04). The /sse path is reverse-proxied to the
 * gateway by Vite in dev and nginx in the Docker image, keeping a single origin.
 *
 * `userId` defaults to "current-user" to match the X-User-Id the upload client
 * sends (apiClient.uploadZipFile), so the deal's uploaded_by_user_id lines up
 * with the stream we subscribe to.
 */
export const useExtractionSocket = (userId: string = 'current-user') => {
  const addNotification = useNotificationStore(
    (state) => state.addNotification,
  );
  const sourceRef = useRef<EventSource | null>(null);
  const retryRef = useRef<ReturnType<typeof setTimeout> | null>(null);

  useEffect(() => {
    let cleanedUp = false;

    const connect = () => {
      const url = `/sse/extraction-updates?userId=${encodeURIComponent(userId)}`;
      console.log('Connecting to Gateway SSE feed...', url);

      const es = new EventSource(url);
      sourceRef.current = es;

      const handleFailure = (event: MessageEvent) => {
        try {
          const payload: ExtractionNotification = JSON.parse(event.data);

          // TC-FE-02: only the "Requires OCR" fallback contract triggers a banner.
          if (payload.status === REQUIRES_OCR_STATUS) {
            addNotification(payload);
          }
        } catch (error) {
          console.error('Error parsing SSE message data:', error);
        }
      };

      es.addEventListener(
        'extraction-failure',
        handleFailure as EventListener,
      );

      es.onerror = () => {
        // TC-FE-01: reconnect after a drop. Close the dead stream first so we
        // never stack duplicate EventSources.
        console.warn('Real-time connection dropped. Reconnecting in 3 seconds...');
        es.close();
        if (!cleanedUp) {
          retryRef.current = setTimeout(connect, 3000);
        }
      };
    };

    connect();

    // Prevent connection leaks when the user logs out or changes routes.
    return () => {
      cleanedUp = true;
      if (retryRef.current) {
        clearTimeout(retryRef.current);
      }
      sourceRef.current?.close();
    };
  }, [userId, addNotification]);
};
