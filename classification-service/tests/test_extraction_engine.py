from unittest.mock import patch, MagicMock

from app.services.extraction_engine import extract_text_with_coordinates, normalize_bbox


class TestNormalizeBbox:
    def test_maps_pixel_coords_to_percentage_scale(self):
        result = normalize_bbox(x0=72, top=100, x1=360, bottom=200, width=720, height=1000)
        assert result == [10.0, 10.0, 50.0, 20.0]

    def test_returns_four_values(self):
        result = normalize_bbox(0, 0, 100, 100, 200, 200)
        assert len(result) == 4


def _make_mock_page(word_text, x0, top, x1, bottom, width=612, height=792):
    mock_page = MagicMock()
    mock_page.width = width
    mock_page.height = height
    mock_page.extract_words.return_value = [
        {'text': word_text, 'x0': x0, 'top': top, 'x1': x1, 'bottom': bottom}
    ]
    return mock_page


class TestExtractTextWithCoordinates:
    """
    TC-AI-01: Engine calculates and returns a [x_min, y_min, x_max, y_max] bbox per word.
    TC-AI-02: Each payload includes source_doc_id and page_number metadata.
    """

    def test_tcAi01_each_word_has_four_element_bbox_within_0_to_100(self):
        mock_page = _make_mock_page('Revenue', x0=72, top=100, x1=180, bottom=120)

        with patch('app.services.extraction_engine.pdfplumber') as mock_pdf:
            mock_pdf.open.return_value.__enter__.return_value.pages = [mock_page]
            results = extract_text_with_coordinates('dummy.pdf', 'doc-1')

        assert len(results) == 1
        bbox = results[0].coordinates.bbox
        assert len(bbox) == 4
        assert all(0.0 <= v <= 100.0 for v in bbox)

    def test_tcAi02_payload_includes_source_doc_id_and_page_number(self):
        mock_page = _make_mock_page('5.0M', x0=10, top=20, x1=80, bottom=30)

        with patch('app.services.extraction_engine.pdfplumber') as mock_pdf:
            mock_pdf.open.return_value.__enter__.return_value.pages = [mock_page]
            results = extract_text_with_coordinates('dummy.pdf', 'doc-42')

        payload = results[0]
        assert payload.source_doc_id == 'doc-42'
        assert payload.coordinates.page_number == 1

    def test_page_number_increments_per_page(self):
        page1 = _make_mock_page('Assets', x0=10, top=10, x1=60, bottom=20)
        page2 = _make_mock_page('Liabilities', x0=10, top=10, x1=80, bottom=20)

        with patch('app.services.extraction_engine.pdfplumber') as mock_pdf:
            mock_pdf.open.return_value.__enter__.return_value.pages = [page1, page2]
            results = extract_text_with_coordinates('dummy.pdf', 'doc-1')

        assert results[0].coordinates.page_number == 1
        assert results[1].coordinates.page_number == 2
