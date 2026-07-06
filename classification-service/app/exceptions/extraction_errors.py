class ExtractionFailureException(Exception):
    """
    Raised when a PDF table cannot be safely extracted.

    Examples:
    - flattened scanned PDF with no text layer
    - corrupted table structure
    - unreadable / overlapping table cells
    """

    def __init__(self, message: str, doc_id: str | None = None):
        self.doc_id = doc_id
        super().__init__(message)
