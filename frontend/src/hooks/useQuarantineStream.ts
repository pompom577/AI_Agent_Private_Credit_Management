import { useEffect } from "react";

export type QuarantinedPayload = {
  quarantine_id: string;
  endpoint: string;
  payload: unknown;
  agent_id: string;
  status: string;
};

export default function useQuarantineStream(
  onNewPayload: (payload: QuarantinedPayload) => void
) {
  useEffect(() => {
    const eventSource = new EventSource("/quarantine/stream");

    eventSource.onmessage = (event) => {
      const newPayload = JSON.parse(event.data);
      onNewPayload(newPayload);
    };

    eventSource.onerror = () => {
      eventSource.close();
    };

    return () => {
      eventSource.close();
    };
  }, [onNewPayload]);
}