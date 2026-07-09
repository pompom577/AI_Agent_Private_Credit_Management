from typing import List, Optional
from pydantic import BaseModel, Field


class BoundingBox(BaseModel):
    page_number: int = Field(..., ge=1)
    bbox: List[float] = Field(
        ...,
        min_length=4,
        max_length=4,
        description='[x_min, y_min, x_max, y_max] normalized to 0-100 scale'
    )
    row_index: Optional[int] = None
    col_index: Optional[int] = None


class CoordinateExtraction(BaseModel):
    text: str
    source_doc_id: str
    coordinates: BoundingBox


class HallucinationValidationResult(BaseModel):
    is_valid: bool
    extracted_text: str
    matched_text: str
    risk_level: str
    reason: str
