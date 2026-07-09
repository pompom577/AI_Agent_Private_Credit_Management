type Props = {
  status: string;
};

export default function TimeoutWarningBadge({ status }: Props) {
  if (status !== "Delivery_Timeout_Warning") {
    return null;
  }

  return (
    <span className="timeout-badge">
      Delivery Timeout Warning
    </span>
  );
}