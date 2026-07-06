#!/usr/bin/env bash
# Migrates the live Neon Postgres database to the new AWS RDS instance
# created by rds.tf — Task #1's "migrate your data to cloud database
# service" screenshot step.
#
# Usage:
#   export SOURCE_DB_URL="postgresql://neondb_owner:<password>@<neon-host>/neondb?sslmode=require"
#   export TARGET_DB_URL="postgresql://platform_admin:<password>@<rds-endpoint>:5432/private_credit_db"
#   ./migrate_neon_to_rds.sh
#
# Run this AFTER `terraform apply` has created the RDS instance (grab its
# endpoint from the `rds_endpoint` output) and BEFORE pointing the gateway
# service at RDS, so the data is already there when it boots.
set -euo pipefail

: "${SOURCE_DB_URL:?Set SOURCE_DB_URL to the Neon connection string}"
: "${TARGET_DB_URL:?Set TARGET_DB_URL to the RDS connection string}"

DUMP_FILE="$(mktemp -d)/neon_dump.sql"

echo "==> Dumping schema + data from Neon..."
# --no-owner/--no-acl: the RDS master user (platform_admin) is a different
# role than Neon's neondb_owner — this avoids "role does not exist" errors
# on restore.
pg_dump "$SOURCE_DB_URL" --no-owner --no-acl --format=plain --file="$DUMP_FILE"

echo "==> Restoring into RDS..."
psql "$TARGET_DB_URL" --file="$DUMP_FILE"

echo "==> Verifying row counts match on both sides..."
for table in deals document_records extracted_metrics document_coordinates metric_audit_log quarantine hitl_audit_ledger; do
  src=$(psql "$SOURCE_DB_URL" -tAc "SELECT count(*) FROM $table")
  dst=$(psql "$TARGET_DB_URL" -tAc "SELECT count(*) FROM $table")
  status="OK"
  if [ "$src" != "$dst" ]; then status="MISMATCH"; fi
  printf "%-22s neon=%-6s rds=%-6s %s\n" "$table" "$src" "$dst" "$status"
done

echo "==> Done. Dump file kept at: $DUMP_FILE"
