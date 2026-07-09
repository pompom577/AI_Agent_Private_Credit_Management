import React, { useEffect, useRef } from 'react';

interface CoordinateOverlayProps {
  bbox: [number, number, number, number];
  pageNumber: number;
}

export default function CoordinateOverlay({
  bbox,
  pageNumber,
}: CoordinateOverlayProps) {
  const highlightRef = useRef<HTMLDivElement>(null);
  const [xMin, yMin, xMax, yMax] = bbox;

  const topPosition = `${yMin}%`;
  const leftPosition = `${xMin}%`;
  const boxWidth = `${xMax - xMin}%`;
  const boxHeight = `${yMax - yMin}%`;

  useEffect(() => {
    if (highlightRef.current) {
      setTimeout(() => {
        highlightRef.current?.scrollIntoView({
          behavior: 'smooth',
          block: 'center',
          inline: 'nearest',
        });
      }, 150);
    }
  }, [bbox, pageNumber]);

  return (
    <div
      ref={highlightRef}
      style={{
        position: 'absolute',
        top: topPosition,
        left: leftPosition,
        width: boxWidth,
        height: boxHeight,
        backgroundColor: 'rgba(255, 193, 7, 0.35)',
        border: '2px solid #ffc107',
        borderRadius: '2px',
        boxShadow: '0 0 8px rgba(255, 193, 7, 0.5)',
        pointerEvents: 'none',
        transition: 'all 0.4s ease-in-out',
        zIndex: 10,
      }}
    />
  );
}
