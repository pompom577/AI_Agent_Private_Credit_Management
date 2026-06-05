import pdfplumber

from app.exceptions.extraction_errors import ExtractionFailureException
from app.models.schemas import ExtractedTable, TableExtractionResult
from app.services.format_validator import preserve_raw_cell


def extract_tables(pdf_path: str, source_doc_id: str) -> TableExtractionResult:
    tables: list[ExtractedTable] = []

    try:
        with pdfplumber.open(pdf_path) as pdf:
            has_text_layer = False

            for page_number, page in enumerate(pdf.pages, start=1):
                text = page.extract_text() or ""

                if text.strip():
                    has_text_layer = True

                page_tables = page.extract_tables()

                for raw_table in page_tables:
                    if not raw_table or len(raw_table) < 2:
                        continue

                    headers = [
                        str(cell).strip() if cell is not None else ""
                        for cell in raw_table[0]
                    ]

                    rows = []
                    for raw_row in raw_table[1:]:
                        row_dict = {}

                        for index, header in enumerate(headers):
                            key = header or f"column_{index + 1}"
                            value = raw_row[index] if index < len(raw_row) else ""

                            # Keep raw text exactly as string.
                            row_dict[key] = preserve_raw_cell(value)

                        rows.append(row_dict)

                    tables.append(
                        ExtractedTable(
                            rows=rows,
                            columns=headers,
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
