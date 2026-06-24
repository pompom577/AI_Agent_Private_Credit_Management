import { render, screen } from '@testing-library/react';
import { describe, it, expect } from 'vitest';
import CoordinateOverlay from './CoordinateOverlay';

/**
 * TC-FE-04: CoordinateOverlay must translate [x_min, y_min, x_max, y_max] into
 * CSS percentage positions so the highlight lands precisely over the source cell.
 * Order matters — a swapped x/y would produce a misaligned box.
 */
describe('CoordinateOverlay', () => {
  it('TC-FE-04: renders highlight div at correct CSS percentage positions from bbox', () => {
    const bbox: [number, number, number, number] = [10, 20, 80, 40];
    const { container } = render(
      <CoordinateOverlay bbox={bbox} pageNumber={1} />,
    );

    const overlay = container.firstChild as HTMLElement;
    expect(overlay.style.left).toBe('10%');
    expect(overlay.style.top).toBe('20%');
    expect(overlay.style.width).toBe('70%');   // xMax - xMin = 80 - 10
    expect(overlay.style.height).toBe('20%');  // yMax - yMin = 40 - 20
  });

  it('TC-FE-04: overlay is positioned absolute so it layers over the PDF page', () => {
    const { container } = render(
      <CoordinateOverlay bbox={[0, 0, 50, 50]} pageNumber={1} />,
    );
    expect((container.firstChild as HTMLElement).style.position).toBe('absolute');
  });
});
