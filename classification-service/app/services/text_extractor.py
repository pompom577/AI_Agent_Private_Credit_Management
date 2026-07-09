import logging
from pathlib import Path

import fitz
from docx import Document

log = logging.getLogger(__name__)

SUPPORTED_EXTENSIONS = {".pdf", ".docx"}


def extract_pdf_text(file_path: str) -> str:
    try:
        with fitz.open(file_path) as pdf:
            return "".join(page.get_text() for page in pdf).strip()
    except Exception as exc:  # corrupted / encrypted / non-PDF masquerading as .pdf
        log.warning("PDF extraction failed for %s: %s", file_path, exc)
        return ""


def extract_docx_text(file_path: str) -> str:
    try:
        doc = Document(file_path)
        return "\n".join(p.text for p in doc.paragraphs).strip()
    except Exception as exc:  # corrupted or non-DOCX file with .docx extension
        log.warning("DOCX extraction failed for %s: %s", file_path, exc)
        return ""


def is_supported(file_path: str) -> bool:
    return Path(file_path).suffix.lower() in SUPPORTED_EXTENSIONS


def extract_text(file_path: str) -> str:
    """Return raw text or an empty string on any failure (graceful fallback)."""
    extension = Path(file_path).suffix.lower()

    if extension == ".pdf":
        return extract_pdf_text(file_path)

    if extension == ".docx":
        return extract_docx_text(file_path)

    return ""