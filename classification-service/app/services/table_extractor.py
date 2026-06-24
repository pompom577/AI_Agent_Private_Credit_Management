import re

import pdfplumber

from app.exceptions.extraction_errors import ExtractionFailureException
from app.models.schemas import ExtractedTable, TableExtractionResult
from app.services.format_validator import preserve_raw_cell

# A financial-statement line is a text label, an optional note-reference integer,
# then the current-period figure. The figure must be comma-grouped (e.g.
# "15,739,443") or a decimal (e.g. "7.1854") so that bare note numbers like "26"
# are never mistaken for the value. Parentheses denote negatives and are kept.
# This line-based parse fits condensed financial statements far better than
# pdfplumber's grid detection, which only fires on tables that have ruled lines.
_METRIC_LINE = re.compile(
    r"^(?P<label>.*?[A-Za-z\)])\s+"
    r"(?:\d{1,3}\s+)?"
    r"(?P<value>\(?-?\d{1,3}(?:,\d{3})+\)?|\(?-?\d+\.\d+\)?)"
)


def _parse_metric_line(line: str) -> dict | None:
    match = _METRIC_LINE.match(line.strip())
    if match is None:
        return None

    label = match.group("label").strip()
    if not any(char.isalpha() for char in label):
        return None

    return {
        "metric": preserve_raw_cell(label),
        "value": preserve_raw_cell(match.group("value").strip()),
    }


def extract_tables(pdf_path: str, source_doc_id: str) -> TableExtractionResult:
    tables: list[ExtractedTable] = []

    try:
        with pdfplumber.open(pdf_path) as pdf:
            has_text_layer = False

            for page_number, page in enumerate(pdf.pages, start=1):
                text = page.extract_text() or ""

                if text.strip():
                    has_text_layer = True

                rows = [
                    row
                    for row in (_parse_metric_line(line) for line in text.splitlines())
                    if row is not None
                ]

                if rows:
                    tables.append(
                        ExtractedTable(
                            rows=rows,
                            columns=["metric", "value"],
                            source_doc_id=source_doc_id,
                            page_number=page_number,
                        )
                    )

            if not has_text_layer:
                raise ExtractionFailureException(
                    "PDF appears to be a flattened image with no readable text layer.",
                    doc_id=source_doc_id,
                )

            if not tables:
                raise ExtractionFailureException(
                    "No readable tables detected in PDF.",
                    doc_id=source_doc_id,
                )

            return TableExtractionResult(tables=tables)

    except ExtractionFailureException:
        raise

    except Exception as exc:
        raise ExtractionFailureException(
            f"Table extraction failed: {str(exc)}",
            doc_id=source_doc_id,
        ) from exc
