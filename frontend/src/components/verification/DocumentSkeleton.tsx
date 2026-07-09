export default function DocumentSkeleton() {
  return (
    <div
      data-testid="document-skeleton"
      style={{
        width: '450px',
        backgroundColor: '#fff',
        borderRadius: '4px',
        padding: '20px',
        display: 'flex',
        flexDirection: 'column',
        gap: '12px',
      }}
    >
      <div
        className="animate-pulse"
        style={{ height: '20px', backgroundColor: '#e9ecef', borderRadius: '4px', width: '45%' }}
      />
      <div
        className="animate-pulse"
        style={{ height: '12px', backgroundColor: '#e9ecef', borderRadius: '4px', width: '65%' }}
      />
      <div
        className="animate-pulse"
        style={{ height: '520px', backgroundColor: '#e9ecef', borderRadius: '4px' }}
      />
      <div
        className="animate-pulse"
        style={{ height: '12px', backgroundColor: '#e9ecef', borderRadius: '4px', width: '30%' }}
      />
    </div>
  );
}
