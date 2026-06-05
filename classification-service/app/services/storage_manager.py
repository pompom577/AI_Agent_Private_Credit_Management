"""S3 download + s3:// URL parsing utilities (Story 1.2, Person 2)."""

import logging
import os
from urllib.parse import urlparse

import boto3
from botocore.exceptions import BotoCoreError, ClientError

log = logging.getLogger(__name__)

AWS_REGION = os.getenv("AWS_REGION", "us-east-1")

_s3_client = None


def _get_s3_client():
    global _s3_client
    if _s3_client is None:
        _s3_client = boto3.client("s3", region_name=AWS_REGION)
    return _s3_client


def parse_s3_url(bucket_url: str) -> tuple[str, str]:
    """
    Parse an ``s3://<bucket>/<key>`` URL into (bucket, key).

    Matches the format emitted by the Java gateway's S3StorageService:
    ``s3://<bucket>/<deal_id>/<filename>``.
    """
    parsed = urlparse(bucket_url)
    if parsed.scheme != "s3" or not parsed.netloc or not parsed.path:
        raise ValueError(f"Invalid S3 URL: {bucket_url!r}")
    return parsed.netloc, parsed.path.lstrip("/")


def download_zip(bucket_url: str, destination_dir: str) -> str:
    """
    Download the ZIP archive referenced by ``bucket_url`` into ``destination_dir``.

    Returns the local file path of the downloaded archive.
    Raises ClientError if the object does not exist (caught upstream so the
    deal can be marked FAILED — see TC referencing 404/Not Found).
    """
    bucket, key = parse_s3_url(bucket_url)
    filename = os.path.basename(key) or "upload.zip"
    local_path = os.path.join(destination_dir, filename)

    log.info("Downloading s3://%s/%s -> %s", bucket, key, local_path)
    try:
        _get_s3_client().download_file(bucket, key, local_path)
    except (ClientError, BotoCoreError) as exc:
        log.error("S3 download failed for %s: %s", bucket_url, exc)
        raise

    return local_path
