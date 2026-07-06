import React from 'react';
import ZipDropzone from '../components/upload/ZipDropzone';

export default function NewDealIngestion() {
  return (
    <div
      className="min-h-screen"
      style={{
        backgroundColor: 'var(--color-bg)',
        color: 'var(--color-text-primary)',
      }}
    >
      <a
        href="#main"
        className="sr-only focus:not-sr-only focus:absolute focus:left-4 focus:top-4 focus:z-50 focus:rounded-md focus:px-4 focus:py-2 focus:text-sm focus:font-semibold focus:text-white"
        style={{ backgroundColor: 'var(--color-accent)' }}
      >
        Skip to main content
      </a>

      <main id="main" className="mx-auto max-w-5xl px-6 py-14">
        {/* Hero */}
        <section className="mb-12">
          <p
            className="mb-3 text-sm font-semibold uppercase tracking-widest"
            style={{ color: 'var(--color-accent)' }}
          >
            New Deal
          </p>
          <h1
            className="text-5xl font-extrabold leading-tight tracking-tight"
            style={{ fontFamily: 'var(--font-display)' }}
          >
            Ingest Documents
          </h1>
          <p
            className="mt-4 max-w-xl text-lg leading-relaxed"
            style={{ color: 'var(--color-text-muted)' }}
          >
            Drop your deal ZIP archive. We'll validate, store, and classify
            every document automatically.
          </p>
        </section>

        {/* Upload card */}
        <section
          className="rounded-2xl border p-8"
          style={{
            backgroundColor: 'var(--color-surface)',
            borderColor: 'var(--color-border)',
            boxShadow: 'var(--shadow-card)',
          }}
          aria-labelledby="upload-heading"
        >
          <div className="mb-6 flex items-start justify-between">
            <div>
              <h2
                id="upload-heading"
                className="text-xl font-bold"
                style={{ fontFamily: 'var(--font-display)' }}
              >
                Upload Documents
              </h2>
              <p
                className="mt-1 text-sm"
                style={{ color: 'var(--color-text-muted)' }}
              >
                ZIP archive only · max 500 MB · PDF, Excel, Word, PowerPoint
                inside
              </p>
            </div>
          </div>

          <ZipDropzone />
        </section>

        {/* Feature cards */}
        <section
          className="mt-8 grid gap-4 sm:grid-cols-3"
          aria-label="Platform features"
        >
          <InfoCard
            icon={
              <svg
                viewBox="0 0 20 20"
                fill="currentColor"
                className="h-5 w-5"
                aria-hidden="true"
              >
                <path d="M4 4a2 2 0 012-2h4.586A2 2 0 0112 2.586L15.414 6A2 2 0 0116 7.414V16a2 2 0 01-2 2H6a2 2 0 01-2-2V4z" />
              </svg>
            }
            title="Supported Formats"
            description="PDF, Excel, Word, and PowerPoint files packed inside a ZIP archive."
          />
          <InfoCard
            icon={
              <svg
                viewBox="0 0 20 20"
                fill="currentColor"
                className="h-5 w-5"
                aria-hidden="true"
              >
                <path
                  fillRule="evenodd"
                  d="M3 4a1 1 0 011-1h12a1 1 0 010 2H4a1 1 0 01-1-1zm0 4a1 1 0 011-1h12a1 1 0 010 2H4a1 1 0 01-1-1zm0 4a1 1 0 011-1h6a1 1 0 010 2H4a1 1 0 01-1-1z"
                  clipRule="evenodd"
                />
              </svg>
            }
            title="Auto Classification"
            description="AI labels documents as Balance Sheets, Cap Tables, NDAs, and more."
          />
          <InfoCard
            icon={
              <svg
                viewBox="0 0 20 20"
                fill="currentColor"
                className="h-5 w-5"
                aria-hidden="true"
              >
                <path d="M9 2a1 1 0 000 2h2a1 1 0 100-2H9z" />
                <path
                  fillRule="evenodd"
                  d="M4 5a2 2 0 012-2 3 3 0 003 3h2a3 3 0 003-3 2 2 0 012 2v11a2 2 0 01-2 2H6a2 2 0 01-2-2V5zm3 4a1 1 0 000 2h.01a1 1 0 100-2H7zm3 0a1 1 0 000 2h3a1 1 0 100-2h-3zm-3 4a1 1 0 100 2h.01a1 1 0 100-2H7zm3 0a1 1 0 100 2h3a1 1 0 100-2h-3z"
                  clipRule="evenodd"
                />
              </svg>
            }
            title="Data Extraction"
            description="Tables and financial metrics are structured and made queryable automatically."
          />
        </section>
      </main>
    </div>
  );
}

function InfoCard({
  icon,
  title,
  description,
}: {
  icon: React.ReactNode;
  title: string;
  description: string;
}) {
  return (
    <div
      className="rounded-xl border p-5 transition-shadow hover:shadow-md"
      style={{
        backgroundColor: 'var(--color-surface)',
        borderColor: 'var(--color-border)',
        boxShadow: 'var(--shadow-card)',
      }}
    >
      <div
        className="mb-3 inline-flex rounded-lg p-2"
        style={{
          backgroundColor: 'var(--color-accent-light)',
          color: 'var(--color-accent)',
        }}
      >
        {icon}
      </div>
      <h3
        className="font-semibold"
        style={{ fontFamily: 'var(--font-display)' }}
      >
        {title}
      </h3>
      <p
        className="mt-2 text-sm leading-relaxed"
        style={{ color: 'var(--color-text-muted)' }}
      >
        {description}
      </p>
    </div>
  );
}
