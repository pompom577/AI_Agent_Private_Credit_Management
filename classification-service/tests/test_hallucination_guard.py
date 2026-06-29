from app.services.hallucination_guard import validate_extraction


class TestHallucinationGuard:
    """
    TC-AI-03: When the LLM generates a value that does not exist inside the
    raw PDF text layer, the validator must flag it as HIGH risk so downstream
    callers can reject or quarantine the extraction.
    """

    def test_tcAi03_value_absent_from_raw_layer_flagged_as_high_risk(self):
        result = validate_extraction(
            extracted_text='$5.0M',
            raw_text_layer='Total Assets 12,000 Net Income 3,500 Cash 800'
        )
        assert result.is_valid is False
        assert result.risk_level == 'HIGH'

    def test_value_present_in_raw_layer_is_valid_and_low_risk(self):
        result = validate_extraction(
            extracted_text='$5.0M',
            raw_text_layer='Revenue $5.0M EBITDA $2.1M'
        )
        assert result.is_valid is True
        assert result.risk_level == 'LOW'

    def test_whitespace_stripped_before_comparison(self):
        result = validate_extraction(
            extracted_text='  $5.0M  ',
            raw_text_layer='Total $5.0M here'
        )
        assert result.is_valid is True
