import { useState } from "react";
import type { QuarantinedPayload } from "../../hooks/useQuarantineStream";
import TimeoutWarningBadge from "./TimeoutWarningBadge";
import { notifyDecisionConfirmed, notifyAuditWriteFailed } from "../../utils/toastNotifier";

type Props = {
  payload: QuarantinedPayload | null;
  onClose: () => void;
  onRemovePayload: (id: string) => void;
};

// Placeholder until real compliance-officer auth exists — same stopgap pattern
// as the "current-user"/"ocr-reviewer" placeholders used elsewhere in this app.
const CURRENT_OFFICER_ID = "11111111-1111-1111-1111-111111111111";

export default function PayloadReviewModal({
  payload,
  onClose,
  onRemovePayload,
}: Props) {
  const [isSubmitting, setIsSubmitting] = useState(false);

  if (payload === null) {
    return null;
  }

  const handleApprove = async () => {
    const confirmApprove = window.confirm(
      "Are you sure you want to approve this payload?"
    );

    if (!confirmApprove) return;

    setIsSubmitting(true);
    try {
      const response = await fetch(`/quarantine/${payload.quarantine_id}/approve`, {
        method: "POST",
        headers: { "X-User-Id": CURRENT_OFFICER_ID },
      });

      if (response.ok) {
        notifyDecisionConfirmed();
        onRemovePayload(payload.quarantine_id);
        onClose();
      } else {
        notifyAuditWriteFailed();
      }
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleReject = async () => {
    const confirmReject = window.confirm(
      "Are you sure you want to reject this payload?"
    );

    if (!confirmReject) return;

    setIsSubmitting(true);
    try {
      const response = await fetch(`/quarantine/${payload.quarantine_id}/reject`, {
        method: "POST",
        headers: { "X-User-Id": CURRENT_OFFICER_ID },
      });

      if (response.ok) {
        notifyDecisionConfirmed();
        onRemovePayload(payload.quarantine_id);
        onClose();
      } else {
        notifyAuditWriteFailed();
      }
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className="modal-background">
      <div className="review-modal">
        <div className="modal-header">
          <h2>Payload Review</h2>
          <button type="button" onClick={onClose}>
            X
          </button>
        </div>

        <p>
          <strong>Quarantine ID:</strong> {payload.quarantine_id}
        </p>
        <p>
          <strong>Target Endpoint:</strong> {payload.endpoint}
        </p>
        <p>
          <strong>Agent ID:</strong> {payload.agent_id}
        </p>
        <p>
          <strong>Status:</strong> {payload.status}
        </p>

        <TimeoutWarningBadge status={payload.status} />

        <h3>JSON Payload</h3>

        <pre className="json-box">
          {JSON.stringify(payload.payload, null, 2)}
        </pre>

        <div className="modal-actions">
          <button
            type="button"
            className="reject-btn"
            onClick={handleReject}
            disabled={isSubmitting}
          >
            Reject
          </button>

          <button
            type="button"
            className="approve-btn"
            onClick={handleApprove}
            disabled={isSubmitting}
          >
            Approve
          </button>
        </div>
      </div>
    </div>
  );
}