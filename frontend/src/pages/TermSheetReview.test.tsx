import { render, screen, waitFor, act } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import TermSheetReview from './TermSheetReview';
import * as lineageApi from '../services/lineageApi';

// DocumentViewer uses react-pdf which requires a real worker — mock to keep tests fast.
// The mock never fires onLoadSuccess/onLoadError, so pdfState stays 'loading',
// which means DocumentSkeleton remains visible — exactly what we assert in TC-FE-02.
vi.mock('../components/verification/DocumentViewer', () => ({
  default: ({ pageNumber }: { pageNumber: number }) => (
    <div data-testid="document-viewer" data-page={pageNumber} />
  ),
}));

const MOCK_METRICS = [
  {
    metric_id: '10',
    metric_name: 'Total Revenue',
    raw_value: '$12,500,000',
    source_doc_id: 3,
    page_number: 1,
    bbox: [20, 25, 45, 29] as [number, number, number, number],
  },
];

describe('TermSheetReview', () => {
  beforeEach(() => {
    vi.restoreAllMocks();
    // Default: backend returns real metrics from DB.
    vi.spyOn(lineageApi, 'fetchAllMetrics').mockResolvedValue(MOCK_METRICS);
  });

  /**
   * TC-FE-01: Clicking "Verify Source →" must open the split-screen audit panel
   * immediately from already-loaded data. No second API call is made.
   */
  it('TC-FE-01: clicking Verify Source opens the split-screen panel with DB data', async () => {
    render(<TermSheetReview />);

    // Wait for fetchAllMetrics to populate the table with real DB data.
    await waitFor(() => {
      expect(screen.getByText('Total Revenue')).toBeInTheDocument();
    });

    await userEvent.click(screen.getByText(/Verify Source/));

    expect(screen.getByText(/Audit Ledger Chain/)).toBeInTheDocument();
  });

  /**
   * TC-FE-02: Clicking a metric row must show a loading skeleton instantly —
   * the skeleton renders from the PDF load state (pdfState='loading') before the
   * document bytes arrive, giving the analyst immediate visual feedback.
   */
  it('TC-FE-02: clicking immediately shows the loading skeleton in the document panel', async () => {
    render(<TermSheetReview />);

    await waitFor(() => {
      expect(screen.getByText('Total Revenue')).toBeInTheDocument();
    });

    await userEvent.click(screen.getByText(/Verify Source/));

    // DocumentSkeleton renders immediately because pdfState starts as 'loading'.
    expect(screen.getByTestId('document-skeleton')).toBeInTheDocument();
  });

  /**
   * TC-FE-01 fallback: when the backend is offline, the table shows demo metrics
   * and clicking still opens the panel (sourceDocId null → demo PDF used).
   */
  it('TC-FE-01 fallback: uses demo metrics when backend is offline', async () => {
    vi.spyOn(lineageApi, 'fetchAllMetrics').mockRejectedValue(new Error('offline'));

    render(<TermSheetReview />);

    await waitFor(() => {
      expect(screen.getByText('Total Revenue')).toBeInTheDocument();
    });

    await userEvent.click(screen.getAllByText(/Verify Source/)[0]);

    expect(screen.getByText(/Audit Ledger Chain/)).toBeInTheDocument();
  });
});
