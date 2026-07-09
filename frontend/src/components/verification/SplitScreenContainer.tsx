import { useState } from "react";
import PdfViewerPanel from "./PdfViewerPanel";

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
  metrics: Metric[];
};

export default function SplitScreenContainer({ metrics }: Props) {
  const [selectedMetric, setSelectedMetric] = useState<Metric | null>(null);

  const handleMetricClick = (metric: Metric) => {
    setSelectedMetric(metric);
  };

  return (
    <div style={{ display: "flex", height: "100vh", width: "100%" }}>
      <div
        style={{
          width: selectedMetric ? "50%" : "100%",
          padding: "20px",
          overflowY: "auto",
          borderRight: selectedMetric ? "1px solid #ddd" : "none",
        }}
      >
        <h2>Digital Term Sheet</h2>

        <table style={{ width: "100%", borderCollapse: "collapse" }}>
          <thead>
            <tr>
              <th>Metric</th>
              <th>Value</th>
            </tr>
          </thead>

          <tbody>
            {metrics.map((metric) => (
              <tr
                key={metric.id}
                onClick={() => handleMetricClick(metric)}
                style={{
                  cursor: "pointer",
                  background:
                    selectedMetric?.id === metric.id ? "#e8f4ff" : "white",
                }}
              >
                <td style={{ border: "1px solid #ddd", padding: "10px" }}>
                  {metric.name}
                </td>
                <td style={{ border: "1px solid #ddd", padding: "10px" }}>
                  {metric.value}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      {selectedMetric && (
        <div style={{ width: "50%", height: "100%", overflowY: "auto" }}>
          <PdfViewerPanel metric={selectedMetric} />
        </div>
      )}
    </div>
  );
}