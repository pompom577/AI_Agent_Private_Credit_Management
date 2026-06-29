type Props = {
  onRetry: () => void;
};

export default function StreamErrorState({ onRetry }: Props) {
  return (
    <div
      style={{
        width: '450px',
        padding: '48px 32px',
        backgroundColor: '#fff',
        borderRadius: '4px',
        textAlign: 'center',
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
        gap: '12px',
      }}
    >
      <svg
        xmlns="http://www.w3.org/2000/svg"
        style={{ width: '48px', height: '48px', color: '#dc3545' }}
        fill="none"
        viewBox="0 0 24 24"
        stroke="currentColor"
        aria-hidden="true"
      >
        <path
          strokeLinecap="round"
          strokeLinejoin="round"
          strokeWidth={1.5}
          d="M12 9v2m0 4h.01M10.29 3.86L1.82 18a2 2 0 001.71 3h16.94a2 2 0 001.71-3L13.71 3.86a2 2 0 00-3.42 0z"
        />
      </svg>

      <h3 style={{ margin: 0, fontSize: '16px', fontWeight: 600, color: 'var(--color-text-primary)' }}>
        Failed to load document
      </h3>

      <p style={{ margin: 0, fontSize: '14px', color: 'var(--color-text-muted)', maxWidth: '280px' }}>
        The source document could not be retrieved. The storage service may have timed out (504).
      </p>

      <button
        onClick={onRetry}
        style={{
          marginTop: '8px',
          padding: '8px 20px',
          backgroundColor: 'var(--color-accent)',
          color: '#fff',
          border: 'none',
          borderRadius: '6px',
          fontWeight: 600,
          cursor: 'pointer',
          fontSize: '14px',
        }}
      >
        Retry
      </button>
    </div>
  );
}
