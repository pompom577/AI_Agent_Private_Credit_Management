import pdfplumber
from app.models.metrics import BoundingBox, CoordinateExtraction


def normalize_bbox(x0: float, top: float, x1: float, bottom: float, width: float, height: float):
    return [
        round((x0 / width) * 100, 2),
        round((top / height) * 100, 2),
        round((x1 / width) * 100, 2),
        round((bottom / height) * 100, 2),
    ]



def extract_text_with_coordinates(pdf_path: str, source_doc_id: str):
    extracted_payload = []

    with pdfplumber.open(pdf_path) as pdf:
        for page_number, page in enumerate(pdf.pages, start=1):
            words = page.extract_words()

            for index, word in enumerate(words):
                bbox = normalize_bbox(
                    word['x0'],
                    word['top'],
                    word['x1'],
                    word['bottom'],
                    page.width,
                    page.height,
                )

                extracted_payload.append(
                    CoordinateExtraction(
                        text=word['text'],
                        source_doc_id=source_doc_id,
                        coordinates=BoundingBox(
                            page_number=page_number,
                            bbox=bbox,
                            row_index=index,
                            col_index=0,
                        ),
                    )
                )

    return extracted_payload
