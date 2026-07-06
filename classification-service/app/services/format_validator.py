"""
This file exists to make sure extracted table values stay exactly as raw strings.
It must not normalize, calculate, convert, or clean financial values.
"""


def preserve_raw_cell(value) -> str:
    """
    Preserve the extracted PDF cell value as a string.

    Examples:
    "$1.5M" stays "$1.5M"
    "(450K)" stays "(450K)"
    "12.5%" stays "12.5%"
    """

    if value is None:
        return ""

    return str(value).strip()


def validate_no_normalization(original_value, preserved_value: str) -> bool:
    """
    Lightweight safety check.
    Returns True if the preserved value matches the original value after string conversion.
    """

    if original_value is None:
        return preserved_value == ""

    return str(original_value).strip() == preserved_value
