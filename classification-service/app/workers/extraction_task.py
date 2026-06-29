# Deprecated since Story 1.2: kept as a thin shim so older imports keep working.
# The real implementation now lives in app.workers.async_pipeline.run_pipeline.
from app.workers.async_pipeline import run_pipeline


def run_extraction(bucket_url: str, deal_id: str, uploaded_by_user_id: str) -> None:
    """Deprecated alias for run_pipeline. Use app.workers.async_pipeline directly."""
    run_pipeline(bucket_url, deal_id, uploaded_by_user_id)