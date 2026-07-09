import { QuarantinedPayload } from "../../hooks/useQuarantineStream";
import TimeoutWarningBadge from "./TimeoutWarningBadge";

type Props = {
  payloads: QuarantinedPayload[];
  onSelectPayload: (payload: QuarantinedPayload) => void;
};

export default function QuarantineQueue({ payloads, onSelectPayload }: Props) {
  if (payloads.length === 0) {
    return <p className="empty-message">No parked payloads waiting for review.</p>;
  }

  return (
    <div className="queue-list">
      {payloads.map((item) => (
        <div
          key={item.quarantine_id}
          className={
            item.status === "Delivery_Timeout_Warning"
              ? "queue-card timeout-warning"
              : "queue-card"
          }
          onClick={() => onSelectPayload(item)}
        >
          <div className="queue-header">
            <h3>Payload #{item.quarantine_id}</h3>
            <TimeoutWarningBadge status={item.status} />
          </div>

          <p><strong>Endpoint:</strong> {item.endpoint}</p>
          <p><strong>Agent ID:</strong> {item.agent_id}</p>
          <p><strong>Status:</strong> {item.status}</p>
        </div>
      ))}
    </div>
  );
}