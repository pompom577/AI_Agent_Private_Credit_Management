interface ErrorBannerProps {
  message: string | null;
}

export default function ErrorBanner({ message }: ErrorBannerProps) {
  if (!message) return null;

  return (
    <div
      role="alert"
      aria-live="assertive"
      className="flex items-start gap-2 rounded-lg border px-4 py-3 text-sm"
      style={{
        backgroundColor: "var(--color-error-bg)",
        borderColor: "var(--color-error-border)",
        color: "var(--color-error)",
      }}
    >
      <span aria-hidden="true" className="mt-0.5 shrink-0">
        ✕
      </span>
      <span>{message}</span>
    </div>
  );
}
