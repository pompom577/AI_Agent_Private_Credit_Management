import { useCallback, useEffect, useState } from "react";
import QuarantineQueue from "./QuarantineQueue";
import PayloadReviewModal from "./PayloadReviewModal";
import useQuarantineStream, {
  QuarantinedPayload,
} from "../../hooks/useQuarantineStream";

export default function HitlDashboard() {
  const [payloads, setPayloads] = useState<QuarantinedPayload[]>([]);
  const [selectedPayload, setSelectedPayload] =
    useState<QuarantinedPayload | null>(null);

  useEffect(() => {
    fetch("/quarantine")
      .then((response) => response.json())
      .then((data) => setPayloads(data))
      .catch(() => alert("Failed to load quarantine queue."));
  }, []);

  const handleNewPayload = useCallback((newPayload: QuarantinedPayload) => {
    setPayloads((currentPayloads) => [
      newPayload,
      ...currentPayloads.filter(
        (item) => item.quarantine_id !== newPayload.quarantine_id
      ),
    ]);
  }, []);

  useQuarantineStream(handleNewPayload);

  function removePayload(id: string) {
    setPayloads((currentPayloads) =>
      currentPayloads.filter((item) => item.quarantine_id !== id)
    );
  }

  return (
    <div className="hitl-dashboard">
      <style>{`
        .hitl-dashboard {
          padding: 30px;
          font-family: Arial, sans-serif;
          background: #f4f6f8;
          min-height: 100vh;
        }

        .dashboard-title {
          font-size: 30px;
          margin-bottom: 5px;
        }

        .dashboard-subtitle {
          color: #666;
          margin-bottom: 25px;
        }

        .queue-list {
          display: grid;
          gap: 15px;
        }

        .queue-card {
          background: white;
          border: 1px solid #ddd;
          border-radius: 12px;
          padding: 18px;
          cursor: pointer;
        }

        .queue-card:hover {
          box-shadow: 0 4px 12px rgba(0,0,0,0.08);
        }

        .timeout-warning {
          border: 2px solid #d62828;
          background: #fff5f5;
        }

        .queue-header {
          display: flex;
          justify-content: space-between;
          align-items: center;
        }

        .timeout-badge {
          background: #d62828;
          color: white;
          padding: 6px 10px;
          border-radius: 20px;
          font-size: 12px;
          font-weight: bold;
        }

        .empty-message {
          background: white;
          padding: 30px;
          border-radius: 12px;
          text-align: center;
          color: #777;
        }

        .modal-background {
          position: fixed;
          inset: 0;
          background: rgba(0,0,0,0.45);
          display: flex;
          align-items: center;
          justify-content: center;
          z-index: 1000;
        }

        .review-modal {
          background: white;
          width: 90%;
          max-width: 750px;
          border-radius: 14px;
          padding: 24px;
          box-shadow: 0 20px 40px rgba(0,0,0,0.2);
        }

        .modal-header {
          display: flex;
          justify-content: space-between;
          align-items: center;
        }

        .modal-header button {
          border: none;
          background: #eee;
          padding: 8px 12px;
          border-radius: 6px;
          cursor: pointer;
        }

        .json-box {
          background: #1e1e1e;
          color: #9cdcfe;
          padding: 15px;
          border-radius: 10px;
          max-height: 300px;
          overflow: auto;
        }

        .modal-actions {
          margin-top: 20px;
          display: flex;
          justify-content: flex-end;
          gap: 12px;
        }

        .reject-btn {
          background: #d62828;
          color: white;
          border: none;
          border-radius: 8px;
          padding: 10px 18px;
          cursor: pointer;
          font-weight: bold;
        }

        .approve-btn {
          background: #2f9e44;
          color: white;
          border: none;
          border-radius: 8px;
          padding: 10px 18px;
          cursor: pointer;
          font-weight: bold;
        }

        .reject-btn:disabled,
        .approve-btn:disabled {
          background: #ccc;
          color: #777;
          cursor: not-allowed;
        }
      `}</style>

      <h1 className="dashboard-title">HITL Compliance Dashboard</h1>
      <p className="dashboard-subtitle">
        Review parked high-risk AI agent payloads before execution.
      </p>

      <QuarantineQueue
        payloads={payloads}
        onSelectPayload={setSelectedPayload}
      />

      <PayloadReviewModal
        payload={selectedPayload}
        onClose={() => setSelectedPayload(null)}
        onRemovePayload={removePayload}
      />
    </div>
  );
}