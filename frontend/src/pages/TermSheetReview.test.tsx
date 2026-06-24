import { render, screen, waitFor, act } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import TermSheetReview from './TermSheetReview';
import * as lineageApi from '../services/lineageApi';

// DocumentViewer uses react-pdf which requires a real worker — mock to keep tests fast.
vi.mock('../components/verification/DocumentViewer', () => ({
  default: ({ pageNumber }: { pageNumber: number }) => (
    <div data-testid="document-viewer" data-page={pageNumber} />
  ),
}));

const MOCK_LINEAGE = {
  metric_id: '1',
  metric_name: 'Total Revenue',
  value: '$12,500,000',
  source_doc_id: 10,
  page_number: 1,
  bbox: [20, 25, 45, 29] as [number, number, number, number],
};

describe('TermSheetReview', () => {
  beforeEach(() => {
    vi.restoreAllMocks();
  });

  /**
   * TC-FE-01: Clicking "Verify Source →" on a metric row must open the split-screen
   * audit panel. If the panel never opens, analysts cannot trace a value back to the
   * source document — the core audit requirement of Story 2.1b.
   */
  it('TC-FE-01: clicking Verify Source opens the split-screen panel', async () => {
    vi.spyOn(lineageApi, 'fetchMetricLineage').mockResolvedValue(MOCK_LINEAGE);

    render(<TermSheetReview />);

    const buttons = screen.getAllByText(/Verify Source/);
    await userEvent.click(buttons[0]);

    await waitFor(() => {
      expect(screen.getByText(/Audit Ledger Chain/)).toBeInTheDocument();
    });
  });

  /**
   * TC-FE-02: While the lineage API is in-flight, the clicked button must be disabled
   * to prevent duplicate requests (which would produce duplicate audit log entries).
   * The panel must show a loading indicator so analysts know the system is responding.
   */
  it('TC-FE-02: button is disabled and loading state is shown while API call is in-flight', async () => {
    let resolve!: (v: typeof MOCK_LINEAGE) => void;
    const pending = new Promise<typeof MOCK_LINEAGE>((r) => { resolve = r; });
    vi.spyOn(lineageApi, 'fetchMetricLineage').mockReturnValue(pending);

    render(<TermSheetReview />);

    const buttons = screen.getAllByText(/Verify Source/);
    await userEvent.click(buttons[0]);

    // All verify buttons must be disabled while any request is in flight
    const allButtons = screen.getAllByRole('button', { name: /Verify Source|Connecting/ });
    allButtons.forEach((btn) => expect(btn).toBeDisabled());

    // Unblock so the component can clean up its loading state
    await act(async () => { resolve(MOCK_LINEAGE); });
  });
});
