from app.models.metrics import HallucinationValidationResult


def validate_extraction(extracted_text: str, raw_text_layer: str):
    normalized_extracted = extracted_text.strip()
    normalized_raw = raw_text_layer.strip()

    if normalized_extracted in normalized_raw:
        return HallucinationValidationResult(
            is_valid=True,
            extracted_text=normalized_extracted,
            matched_text=normalized_extracted,
            risk_level='LOW',
            reason='Exact text match found inside raw PDF text layer.'
        )

    return HallucinationValidationResult(
        is_valid=False,
        extracted_text=normalized_extracted,
        matched_text='',
        risk_level='HIGH',
        reason='Potential hallucination detected. Extracted value does not exist in source bounding box.'
    )
