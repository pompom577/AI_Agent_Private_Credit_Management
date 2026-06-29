"""
1.2 Classification:
  1. Download ZIP from S3
  2. Unzip into ephemeral /tmp workspace
  3. Extract text
  4. Classify document
  5. UPSERT DocumentRecord (records file_path for the 1.3 batch)
  6. Emit classification-complete callback

1.3 Tabular Extraction:
  1. Find classified financial PDFs
  2. Extract tables (strict raw-value preservation)
  3. Insert extracted metrics
  4. Mark document as Extracted
  5. If extraction fails, mark document as Extraction_Failed_Requires_OCR
  6. Emit extraction failure event to the Spring Boot gateway
  7. Continue processing the rest of the batch (per-document isolation)
"""


import logging
import os
import shutil
import tempfile
import zipfile

import httpx
from botocore.exceptions import BotoCoreError, ClientError
from sqlalchemy.exc import SQLAlchemyError

from app.core.config import GATEWAY_CALLBACK_URL, GATEWAY_EXTRACTION_EVENT_URL
from app.db.database import SessionLocal
from app.db.models import DocumentRecord
from app.exceptions.extraction_errors import ExtractionFailureException
from app.models.extracted_metric import ExtractedMetric
from app.models.schemas import ExtractionFailureEvent
from app.services.classification_engine import classify_document
from app.services.storage_manager import download_zip, parse_s3_url, upload_file
from app.services.table_extractor import extract_tables
from app.services.text_extractor import extract_text, is_supported

log = logging.getLogger(__name__)

# Story 1.2 classification statuses (mirrored on the gateway side).
STATUS_CLASSIFIED = "CLASSIFIED"
STATUS_FAILED = "FAILED"
STATUS_UNSUPPORTED = "UNSUPPORTED"
STATUS_PENDING = "PENDING"

# Story 1.3 extraction statuses — exact strings per the spec; the React client
# filters on "Extraction_Failed_Requires_OCR" and the audit chain reads "Extracted".
STATUS_EXTRACTED = "Extracted"
STATUS_EXTRACTION_FAILED_REQUIRES_OCR = "Extraction_Failed_Requires_OCR"

# Categories the classifier can emit that warrant tabular extraction. The
# classifier (app/core/prompts.py) only returns Balance Sheet / Cap Table / NDA /
# Unsupported, so only the two financial labels below ever match.
FINANCIAL_CATEGORIES = {
    "Balance Sheet",
    "Cap Table",
}


def _upsert_document_record(
    deal_id: str,
    filename: str,
    category: str | None,
    status: str,
    file_path: str | None = None,
) -> None:
    session = SessionLocal()

    try:
        existing = (
            session.query(DocumentRecord)
            .filter_by(deal_id=deal_id, filename=filename)
            .one_or_none()
        )

        if existing is None:
            session.add(DocumentRecord(
                deal_id=deal_id,
                filename=filename,
                category=category,
                status=status,
                file_path=file_path,
            ))
        else:
            existing.category = category
            existing.status = status
            if file_path is not None:
                existing.file_path = file_path

        session.commit()

    except SQLAlchemyError:
        session.rollback()
        log.exception("DB upsert failed for deal=%s filename=%s", deal_id, filename)
        raise

    finally:
        session.close()


def _send_classification_complete(
    deal_id: str,
    status: str,
    total_documents: int,
) -> None:
    payload = {
        "deal_id": deal_id,
        "status": status,
        "total_documents": total_documents,
    }

    try:
        with httpx.Client(timeout=10.0) as client:
            response = client.post(GATEWAY_CALLBACK_URL, json=payload)
            response.raise_for_status()

    except httpx.HTTPError as exc:
        log.error("Classification callback failed for deal=%s: %s", deal_id, exc)


def _send_extraction_failure_event(event: ExtractionFailureEvent) -> None:
    """POST {doc_id, deal_id, filename, status} to the gateway so it can route
    an SSE notification to the analyst who owns the deal (TC-BAT-04 / TC-GW-03)."""
    payload = event.model_dump()

    try:
        with httpx.Client(timeout=10.0) as client:
            response = client.post(GATEWAY_EXTRACTION_EVENT_URL, json=payload)
            response.raise_for_status()

    except httpx.HTTPError as exc:
        log.error(
            "Extraction failure event failed for doc=%s: %s",
            event.doc_id,
            exc,
        )


def _process_workspace(deal_id: str, extract_dir: str) -> int:
    """Phase 1: classify every file and store the LOCAL path in the DB.
    The local path is used by run_tabular_extraction_batch (Phase 2).
    _upload_documents_and_update_paths (Phase 3) then swaps it to the S3 URL."""
    processed = 0

    for root, _dirs, files in os.walk(extract_dir):
        for filename in files:
            local_path = os.path.join(root, filename)

            try:
                if not is_supported(local_path):
                    _upsert_document_record(
                        deal_id=deal_id,
                        filename=filename,
                        category=None,
                        status=STATUS_UNSUPPORTED,
                        file_path=local_path,
                    )
                    processed += 1
                    continue

                text = extract_text(local_path)
                category = classify_document(text)

                status = (
                    STATUS_CLASSIFIED
                    if category and category != "Unsupported"
                    else STATUS_UNSUPPORTED
                )

                _upsert_document_record(
                    deal_id=deal_id,
                    filename=filename,
                    category=category,
                    status=status,
                    file_path=local_path,
                )

                processed += 1

            except Exception:
                log.exception(
                    "Per-document classification failure: deal=%s filename=%s",
                    deal_id,
                    filename,
                )

                try:
                    _upsert_document_record(
                        deal_id=deal_id,
                        filename=filename,
                        category=None,
                        status=STATUS_FAILED,
                        file_path=local_path,
                    )
                except Exception:
                    log.exception("Failed to record FAILED status for %s", filename)

                processed += 1

    return processed


def _upload_documents_and_update_paths(deal_id: str, bucket: str) -> None:
    """Phase 3: upload each extracted file to S3 and replace the local file_path
    in document_records with the permanent s3:// URL.

    Must run after tabular extraction (which needs the local path) and before
    the workspace is wiped by the finally block in run_pipeline.
    """
    session = SessionLocal()
    try:
        docs = (
            session.query(DocumentRecord)
            .filter(DocumentRecord.deal_id == deal_id)
            .all()
        )
        for doc in docs:
            local_path = doc.file_path
            if not local_path or local_path.startswith("s3://") or not os.path.exists(local_path):
                continue
            s3_key = f"{deal_id}/documents/{doc.filename}"
            try:
                doc.file_path = upload_file(local_path, bucket, s3_key)
                session.add(doc)
            except Exception:
                log.exception(
                    "S3 upload failed for deal=%s filename=%s; file_path left as local",
                    deal_id,
                    doc.filename,
                )
        session.commit()
    except SQLAlchemyError:
        session.rollback()
        log.exception("Failed to update S3 file paths for deal=%s", deal_id)
    finally:
        session.close()


def run_pipeline(
    bucket_url: str,
    deal_id: str,
    uploaded_by_user_id: str,
) -> None:
    log.info(
        "Starting pipeline: deal_id=%s, bucket_url=%s, uploaded_by=%s",
        deal_id,
        bucket_url,
        uploaded_by_user_id,
    )

    workspace = tempfile.mkdtemp(prefix=f"deal-{deal_id}-")
    final_status = STATUS_CLASSIFIED
    total_documents = 0

    try:
        try:
            local_zip = download_zip(bucket_url, workspace)

        except (ClientError, BotoCoreError, ValueError):
            log.exception("S3 download failed; marking deal=%s as FAILED", deal_id)
            final_status = STATUS_FAILED
            return

        extract_dir = os.path.join(workspace, "extracted")
        os.makedirs(extract_dir, exist_ok=True)

        try:
            with zipfile.ZipFile(local_zip) as zip_file:
                zip_file.extractall(extract_dir)

        except zipfile.BadZipFile:
            log.exception("Corrupt ZIP for deal=%s", deal_id)
            final_status = STATUS_FAILED
            return

        total_documents = _process_workspace(deal_id=deal_id, extract_dir=extract_dir)

        if total_documents == 0:
            final_status = STATUS_UNSUPPORTED

        # Phase 2: tabular extraction reads local file paths stored by Phase 1.
        # Must finish before the finally block wipes the workspace.
        run_tabular_extraction_batch(deal_id)

        # Phase 3: upload each extracted file to S3 and update file_path to the
        # permanent s3:// URL so StorageStreamService can stream it to the frontend.
        bucket, _ = parse_s3_url(bucket_url)
        _upload_documents_and_update_paths(deal_id=deal_id, bucket=bucket)

    finally:
        shutil.rmtree(workspace, ignore_errors=True)
        log.info("Wiped ephemeral workspace for deal=%s", deal_id)

        _send_classification_complete(
            deal_id=deal_id,
            status=final_status,
            total_documents=total_documents,
        )


def run_tabular_extraction_batch(deal_id: str) -> None:
    """Iterate classified financial PDFs for the deal, extract tables, and persist
    metrics. Failures are isolated per-document (TC-BAT-01): a bad PDF is flagged
    for OCR and the loop continues with the next one."""
    session = SessionLocal()

    try:
        docs = (
            session.query(DocumentRecord)
            .filter(DocumentRecord.deal_id == deal_id)
            .filter(DocumentRecord.category.in_(FINANCIAL_CATEGORIES))
            .filter(DocumentRecord.status == STATUS_CLASSIFIED)
            .all()
        )

        log.info(
            "Starting tabular extraction batch for deal=%s documents=%s",
            deal_id,
            len(docs),
        )

        for doc in docs:
            try:
                pdf_path = doc.file_path

                if not pdf_path:
                    raise ExtractionFailureException(
                        f"Document {doc.filename} has no file_path.",
                        doc_id=str(doc.id),
                    )

                result = extract_tables(pdf_path=pdf_path, source_doc_id=str(doc.id))

                _save_extracted_metrics(session, result)

                doc.status = STATUS_EXTRACTED
                session.add(doc)
                session.commit()

                log.info(
                    "Tabular extraction succeeded: deal=%s doc=%s",
                    deal_id,
                    doc.filename,
                )

            except ExtractionFailureException:
                _handle_extraction_failure(session, deal_id, doc)
                continue

            except Exception:
                log.exception(
                    "Unexpected extraction error: deal=%s doc=%s",
                    deal_id,
                    doc.filename,
                )
                _handle_extraction_failure(session, deal_id, doc)
                continue

    finally:
        session.close()


def _handle_extraction_failure(session, deal_id: str, doc: DocumentRecord) -> None:
    """Roll back, flag the document for OCR, and notify the gateway (TC-BAT-03/04)."""
    session.rollback()

    log.warning(
        "Tabular extraction failed and requires OCR: deal=%s doc=%s",
        deal_id,
        doc.filename,
    )

    doc.status = STATUS_EXTRACTION_FAILED_REQUIRES_OCR
    session.add(doc)
    session.commit()

    event = ExtractionFailureEvent(
        doc_id=str(doc.id),
        filename=doc.filename,
        deal_id=str(doc.deal_id),
        uploaded_by_user_id=str(getattr(doc, "uploaded_by_user_id", "")),
        status=STATUS_EXTRACTION_FAILED_REQUIRES_OCR,
    )

    _send_extraction_failure_event(event)


def _save_extracted_metrics(session, result) -> None:
    for table in result.tables:
        for row in table.rows:
            metric_name = _infer_metric_name(row)
            raw_value = _infer_raw_value(row)
            unit = _infer_unit(row)

            metric = ExtractedMetric(
                metric_name=metric_name,
                raw_value=raw_value,
                unit=unit,
                source_doc_id=int(table.source_doc_id),
                page_number=table.page_number,
            )

            session.add(metric)

    session.commit()


def _infer_metric_name(row: dict) -> str:
    keys = list(row.keys())

    if not keys:
        return "Unknown Metric"

    first_key = keys[0]
    first_value = row.get(first_key)

    if first_value:
        return str(first_value).strip()

    return str(first_key).strip()


def _infer_raw_value(row: dict) -> str:
    keys = list(row.keys())

    if len(keys) >= 2:
        return str(row.get(keys[1], "")).strip()

    if len(keys) == 1:
        return str(row.get(keys[0], "")).strip()

    return ""


def _infer_unit(row: dict) -> str | None:
    for key, value in row.items():
        key_lower = str(key).lower()

        if "unit" in key_lower or "currency" in key_lower:
            return str(value).strip()

    return None
