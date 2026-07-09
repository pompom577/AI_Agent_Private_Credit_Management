from datetime import datetime, timezone

from sqlalchemy import DateTime, String, UniqueConstraint, func
from sqlalchemy.orm import Mapped, mapped_column

from app.db.database import Base


class DocumentRecord(Base):
    """ORM mapping for the document_records table (Story 1.2, Person 3)."""

    __tablename__ = "document_records"
    __table_args__ = (
        UniqueConstraint("deal_id", "filename", name="uq_document_records_deal_filename"),
    )

    id: Mapped[int] = mapped_column(primary_key=True, autoincrement=True)
    deal_id: Mapped[str] = mapped_column(
        String(255), nullable=False, index=True
    )
    filename: Mapped[str] = mapped_column(String(500), nullable=False)
    category: Mapped[str | None] = mapped_column(String(100), nullable=True)
    status: Mapped[str] = mapped_column(String(50), nullable=False, default="PENDING")
    # Absolute path to the extracted file in the ephemeral workspace. Written
    # during classification so the Story 1.3 tabular-extraction batch can re-open
    # the PDF within the same pipeline run (before the workspace is wiped).
    file_path: Mapped[str | None] = mapped_column(String(1000), nullable=True)
    created_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True), server_default=func.now(), nullable=False
    )
    updated_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True),
        server_default=func.now(),
        onupdate=func.now(),
        nullable=False,
    )

    def __repr__(self) -> str:
        return (
            f"<DocumentRecord id={self.id} deal_id={self.deal_id!r} "
            f"filename={self.filename!r} category={self.category!r} status={self.status!r}>"
        )
