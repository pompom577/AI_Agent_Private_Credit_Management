from sqlalchemy import BigInteger, Column, DateTime, ForeignKey, Integer, String, Text, func

from app.db.database import Base


class ExtractedMetric(Base):
    """ORM mapping for the extracted_metrics table (Story 1.3).

    Mirrors infrastructure/init-scripts/03-extracted-metrics-schema.sql:
    metric_name/raw_value are NOT NULL; source_doc_id FKs document_records(id).
    """

    __tablename__ = "extracted_metrics"

    id = Column(BigInteger, primary_key=True, index=True)

    metric_name = Column(String(500), nullable=False)
    raw_value = Column(String(500), nullable=False)
    unit = Column(String(100), nullable=True)

    source_doc_id = Column(
        BigInteger,
        ForeignKey("document_records.id", ondelete="CASCADE"),
        nullable=False,
        index=True,
    )

    page_number = Column(Integer, nullable=True)

    created_at = Column(DateTime(timezone=True), server_default=func.now())
