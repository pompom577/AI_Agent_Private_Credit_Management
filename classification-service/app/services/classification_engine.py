import logging

from llama_index.core.llms import ChatMessage

from app.core.llm_config import llm
from app.core.prompts import SYSTEM_PROMPT

log = logging.getLogger(__name__)

SUPPORTED_CATEGORIES = [
    "Balance Sheet",
    "Cap Table",
    "NDA",
    "Unsupported",
]


def classify_document(text: str) -> str:
    """Classify a document; always falls back to 'Unsupported' on any failure."""

    if not text or len(text.strip()) < 20:
        return "Unsupported"

    try:
        response = llm.chat([
            ChatMessage(role="system", content=SYSTEM_PROMPT),
            ChatMessage(role="user", content=text[:4000]),
        ])
    except Exception as exc:  # network / auth / rate-limit
        log.warning("LLM classification call failed: %s", exc)
        return "Unsupported"

    content = getattr(response.message, "content", None)
    if not content:
        return "Unsupported"

    category = content.strip()
    if category not in SUPPORTED_CATEGORIES:
        return "Unsupported"

    return category