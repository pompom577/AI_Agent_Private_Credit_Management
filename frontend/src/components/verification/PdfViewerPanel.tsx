import { useEffect, useRef, useState } from "react";
import { Document, Page, pdfjs } from "react-pdf";
import "react-pdf/dist/Page/TextLayer.css";
import "react-pdf/dist/Page/AnnotationLayer.css";

pdfjs.GlobalWorkerOptions.workerSrc = new URL(
  "pdfjs-dist/build/pdf.worker.min.mjs",
  import.meta.url
).toString();

type Metric = {
  id: string;
  name: string;
  value: string;
  source_doc_id: string;
  page_number: number;
  x_min: number;
  y_min: number;
  x_max: number;
  y_max: number;
};

type Props = {
  metric: Metric;
};

export default function PdfViewerPanel({ metric }: Props) {
  const [pdfUrl, setPdfUrl] = useState<string | null>(null);
  const [pageSize, setPageSize] = useState({ width: 0, height: 0 });
  const highlightRef = useRef<HTMLDivElement | null>(null);

  useEffect(() => {
    const url = `/documents/${metric.source_doc_id}/page/${metric.page_number}`;
    setPdfUrl(url);
  }, [metric]);

  useEffect(() => {
    if (highlightRef.current) {
      highlightRef.current.scrollIntoView({
        behavior: "smooth",
        block: "center",
      });
    }
  }, [pageSize]);

  const scaleX = pageSize.width / 612;
  const scaleY = pageSize.height / 792;

  const highlightStyle = {
    position: "absolute" as const,
    left: metric.x_min * scaleX,
    top: metric.y_min * scaleY,
    width: (metric.x_max - metric.x_min) * scaleX,
    height: (metric.y_max - metric.y_min) * scaleY,
    backgroundColor: "rgba(255, 255, 0, 0.4)",
    border: "2px solid orange",
    pointerEvents: "none" as const,
  };

  return (
    <div style={{ padding: "20px" }}>
      <h3>Source Document</h3>

      {pdfUrl && (
        <Document file={pdfUrl}>
          <div style={{ position: "relative", display: "inline-block" }}>
            <Page
              pageNumber={1}
              width={600}
              onRenderSuccess={(page) => {
                setPageSize({
                  width: page.width,
                  height: page.height,
                });
              }}
            />

            {pageSize.width > 0 && (
              <div ref={highlightRef} style={highlightStyle}></div>
            )}
          </div>
        </Document>
      )}
    </div>
  );
}