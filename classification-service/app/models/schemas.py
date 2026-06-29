# Strict payload contract shared between gateway and FastAPI service
from typing import Any
from pydantic import BaseModel, Field


class ClassifyRequest(BaseModel):
    bucket_url: str = Field(..., min_length=1)
    deal_id: str = Field(..., min_length=1)
    uploaded_by_user_id: str = Field(..., min_length=1)


class ExtractedTable(BaseModel):
    rows: list[dict[str, Any]]
    columns: list[str]
    source_doc_id: str
    page_number: int


class TableExtractionResult(BaseModel):
    tables: list[ExtractedTable]


class ExtractionFailureEvent(BaseModel):
    doc_id: str
    filename: str
    deal_id: str
    uploaded_by_user_id: str
    status: str = Field(default="Extraction_Failed_Requires_OCR")
