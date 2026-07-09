import os
import time

from dotenv import load_dotenv
from sqlalchemy import create_engine, text
from sqlalchemy.orm import DeclarativeBase, sessionmaker

load_dotenv()

DATABASE_URL = os.getenv(
    "DATABASE_URL",
    "postgresql://platform_admin:ChooseAHighlySecureRandomPassword123!@localhost:5432/private_credit_db",
)

# Task #1 dual-DB: when set (the Neon URL), sessions fail over to it while the
# primary (AWS RDS) is unreachable — mirrors the gateway's
# FailoverRoutingDataSource so the worker keeps writing during an RDS outage.
BACKUP_DATABASE_URL = os.getenv("BACKUP_DATABASE_URL") or None

_CONNECT_ARGS = {"connect_timeout": 5}
_HEALTH_CHECK_INTERVAL_S = 5.0

engine = create_engine(DATABASE_URL, pool_pre_ping=True, connect_args=_CONNECT_ARGS)
_primary_sessionmaker = sessionmaker(autocommit=False, autoflush=False, bind=engine)

backup_engine = (
    create_engine(BACKUP_DATABASE_URL, pool_pre_ping=True, connect_args=_CONNECT_ARGS)
    if BACKUP_DATABASE_URL
    else None
)
_backup_sessionmaker = (
    sessionmaker(autocommit=False, autoflush=False, bind=backup_engine)
    if backup_engine
    else None
)

_primary_up = True
_last_probe_at = 0.0


def _primary_healthy() -> bool:
    """Probe the primary at most once per interval; reuse the last result between probes."""
    global _primary_up, _last_probe_at
    now = time.monotonic()
    if now - _last_probe_at >= _HEALTH_CHECK_INTERVAL_S:
        _last_probe_at = now
        try:
            with engine.connect() as conn:
                conn.execute(text("SELECT 1"))
            _primary_up = True
        except Exception:
            _primary_up = False
    return _primary_up


def SessionLocal():
    """Session factory: primary (RDS) while healthy, backup (Neon) otherwise.

    Kept callable as ``SessionLocal()`` so existing call sites (async_pipeline)
    work unchanged.
    """
    if _backup_sessionmaker is not None and not _primary_healthy():
        return _backup_sessionmaker()
    return _primary_sessionmaker()


class Base(DeclarativeBase):
    pass


def get_db():
    db = SessionLocal()
    try:
        yield db
    finally:
        db.close()
