import { render, screen } from '@testing-library/react';
import { describe, it, expect, vi } from 'vitest';
import DocumentViewer from './DocumentViewer';

// react-pdf requires a real PDF.js worker and binary file — mock the module so
// we can verify that DocumentViewer passes the correct pageNumber prop to Page.
vi.mock('react-pdf', () => ({
  pdfjs: { GlobalWorkerOptions: {}, version: '3.0.0' },
  Document: ({ children }: { children: React.ReactNode }) => (
    <div data-testid="pdf-document">{children}</div>
  ),
  Page: ({ pageNumber }: { pageNumber: number }) => (
    <div data-testid="pdf-page" data-page={pageNumber} />
  ),
}));

/**
 * TC-FE-03: DocumentViewer must navigate to the stored page_number so the analyst
 * lands on the exact source page without scrolling through the whole document.
 * A wrong page would break the spatial audit chain from metric → document location.
 */
describe('DocumentViewer', () => {
  it('TC-FE-03: renders the correct PDF page using the stored page_number', () => {
    render(<DocumentViewer pageNumber={3} bbox={[10, 20, 80, 40]} />);

    const page = screen.getByTestId('pdf-page');
    expect(page).toHaveAttribute('data-page', '3');
  });

  it('TC-FE-03: passes bbox to CoordinateOverlay for precise highlighting', () => {
    const { container } = render(
      <DocumentViewer pageNumber={1} bbox={[20, 25, 45, 29]} />,
    );
    // CoordinateOverlay renders as an absolute-positioned div inside the page container
    const overlays = container.querySelectorAll('[style*="position: absolute"]');
    expect(overlays.length).toBeGreaterThan(0);
  });
});
