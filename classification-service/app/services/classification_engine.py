import logging
import time

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

# Gemini returns transient 503 (high demand) / 429 (rate limit) under load.
# Retry with linear backoff before giving up so a temporary blip does not
# silently misclassify a real financial document as "Unsupported".
_MAX_ATTEMPTS = 3
_RETRY_BACKOFF_SECONDS = 2


def classify_document(text: str) -> str:
    """Classify a document; always falls back to 'Unsupported' on any failure."""

    if not text or len(text.strip()) < 20:
        return "Unsupported"

    response = None
    for attempt in range(1, _MAX_ATTEMPTS + 1):
        try:
            response = llm.chat([
                ChatMessage(role="system", content=SYSTEM_PROMPT),
                ChatMessage(role="user", content=text[:4000]),
            ])
            break
        except Exception as exc:  # network / auth / rate-limit / transient 503
            log.warning(
                "LLM classification attempt %s/%s failed: %s",
                attempt, _MAX_ATTEMPTS, exc,
            )
            if attempt < _MAX_ATTEMPTS:
                time.sleep(_RETRY_BACKOFF_SECONDS * attempt)

    if response is None:
        return "Unsupported"

    content = getattr(response.message, "content", None)
    if not content:
        return "Unsupported"

    category = content.strip()
    if category not in SUPPORTED_CATEGORIES:
        return "Unsupported"

    return category