-- Repairs 28 Neon rows overwritten on 2026-07-07 by a BackupSyncService test run
-- that synced from an EMPTY primary (migration step skipped): document_records ids 1-2
-- and extracted_metrics ids 1-26 were clobbered by colliding serial ids, and one test
-- deal row was inserted. Original values below come from terraform/migration_dump.sql
-- (Neon dump taken 2026-07-06). Apply with:
--   psql "$NEON_URL" -v ON_ERROR_STOP=1 -f neon_repair_2026-07-07.sql
-- NOTE: document_records.updated_at will be reset to now() by the table's trigger.
BEGIN;
UPDATE document_records SET deal_id = 'b6526af7-e797-4290-98c8-0aff286ecb06', filename = 'PGB Quarterly Report for the First Quarter Ended 31 March 2026.pdf', category = 'Unsupported', status = 'UNSUPPORTED', created_at = '2026-06-05 01:32:17.708379', file_path = '/tmp/deal-b6526af7-e797-4290-98c8-0aff286ecb06-nzvm2w46/extracted/PGB Quarterly Report for the First Quarter Ended 31 March 2026.pdf', page_count = NULL WHERE id = 1;
UPDATE document_records SET deal_id = 'b6526af7-e797-4290-98c8-0aff286ecb06', filename = '._PGB Quarterly Report for the First Quarter Ended 31 March 2026.pdf', category = 'Unsupported', status = 'UNSUPPORTED', created_at = '2026-06-05 01:32:18.972348', file_path = '/tmp/deal-b6526af7-e797-4290-98c8-0aff286ecb06-nzvm2w46/extracted/__MACOSX/._PGB Quarterly Report for the First Quarter Ended 31 March 2026.pdf', page_count = NULL WHERE id = 2;
UPDATE extracted_metrics SET metric_name = 'QUARTERLY REPORT', raw_value = 'QUARTERLY REPORT', unit = NULL, source_doc_id = '5', page_number = '1', created_at = '2026-06-05 01:50:10.351686+00' WHERE id = 1;
UPDATE extracted_metrics SET metric_name = 'FOR THE FIRST QUARTER ENDED 31 MARCH 2026', raw_value = 'FOR THE FIRST QUARTER ENDED 31 MARCH 2026', unit = NULL, source_doc_id = '5', page_number = '1', created_at = '2026-06-05 01:50:10.351686+00' WHERE id = 2;
UPDATE extracted_metrics SET metric_name = 'PETRONAS GAS BERHAD (198301006447 (101671-H))', raw_value = '', unit = NULL, source_doc_id = '5', page_number = '1', created_at = '2026-06-05 01:50:10.351686+00' WHERE id = 3;
UPDATE extracted_metrics SET metric_name = 'Property, plant and equipment', raw_value = '15,739,443', unit = NULL, source_doc_id = '9', page_number = '1', created_at = '2026-06-05 02:04:48.268157+00' WHERE id = 4;
UPDATE extracted_metrics SET metric_name = 'Investments in joint ventures', raw_value = '1,148,015', unit = NULL, source_doc_id = '9', page_number = '1', created_at = '2026-06-05 02:04:48.268157+00' WHERE id = 5;
UPDATE extracted_metrics SET metric_name = 'Investments in associate', raw_value = '230,218', unit = NULL, source_doc_id = '9', page_number = '1', created_at = '2026-06-05 02:04:48.268157+00' WHERE id = 6;
UPDATE extracted_metrics SET metric_name = 'Long-term receivables', raw_value = '40,961', unit = NULL, source_doc_id = '9', page_number = '1', created_at = '2026-06-05 02:04:48.268157+00' WHERE id = 7;
UPDATE extracted_metrics SET metric_name = 'TOTAL NON-CURRENT ASSETS', raw_value = '17,158,637', unit = NULL, source_doc_id = '9', page_number = '1', created_at = '2026-06-05 02:04:48.268157+00' WHERE id = 8;
UPDATE extracted_metrics SET metric_name = 'Trade and other inventories', raw_value = '36,220', unit = NULL, source_doc_id = '9', page_number = '1', created_at = '2026-06-05 02:04:48.268157+00' WHERE id = 9;
UPDATE extracted_metrics SET metric_name = 'Trade and other receivables', raw_value = '914,609', unit = NULL, source_doc_id = '9', page_number = '1', created_at = '2026-06-05 02:04:48.268157+00' WHERE id = 10;
UPDATE extracted_metrics SET metric_name = 'Tax recoverable', raw_value = '14,755', unit = NULL, source_doc_id = '9', page_number = '1', created_at = '2026-06-05 02:04:48.268157+00' WHERE id = 11;
UPDATE extracted_metrics SET metric_name = 'Cash and cash equivalents', raw_value = '1,515,943', unit = NULL, source_doc_id = '9', page_number = '1', created_at = '2026-06-05 02:04:48.268157+00' WHERE id = 12;
UPDATE extracted_metrics SET metric_name = 'TOTAL CURRENT ASSETS', raw_value = '2,481,527', unit = NULL, source_doc_id = '9', page_number = '1', created_at = '2026-06-05 02:04:48.268157+00' WHERE id = 13;
UPDATE extracted_metrics SET metric_name = 'TOTAL ASSETS', raw_value = '19,640,164', unit = NULL, source_doc_id = '9', page_number = '1', created_at = '2026-06-05 02:04:48.268157+00' WHERE id = 14;
UPDATE extracted_metrics SET metric_name = 'Share capital', raw_value = '3,165,204', unit = NULL, source_doc_id = '9', page_number = '1', created_at = '2026-06-05 02:04:48.268157+00' WHERE id = 15;
UPDATE extracted_metrics SET metric_name = 'Reserves', raw_value = '11,052,678', unit = NULL, source_doc_id = '9', page_number = '1', created_at = '2026-06-05 02:04:48.268157+00' WHERE id = 16;
UPDATE extracted_metrics SET metric_name = 'Total equity attributable to the shareholders of the Company', raw_value = '14,217,882', unit = NULL, source_doc_id = '9', page_number = '1', created_at = '2026-06-05 02:04:48.268157+00' WHERE id = 17;
UPDATE extracted_metrics SET metric_name = 'Non-controlling interests', raw_value = '528,569', unit = NULL, source_doc_id = '9', page_number = '1', created_at = '2026-06-05 02:04:48.268157+00' WHERE id = 18;
UPDATE extracted_metrics SET metric_name = 'TOTAL EQUITY', raw_value = '14,746,451', unit = NULL, source_doc_id = '9', page_number = '1', created_at = '2026-06-05 02:04:48.268157+00' WHERE id = 19;
UPDATE extracted_metrics SET metric_name = 'Borrowings', raw_value = '2,199,484', unit = NULL, source_doc_id = '9', page_number = '1', created_at = '2026-06-05 02:04:48.268157+00' WHERE id = 20;
UPDATE extracted_metrics SET metric_name = 'Deferred tax liabilities', raw_value = '1,318,624', unit = NULL, source_doc_id = '9', page_number = '1', created_at = '2026-06-05 02:04:48.268157+00' WHERE id = 21;
UPDATE extracted_metrics SET metric_name = 'Other long-term liabilities and provisions', raw_value = '54,359', unit = NULL, source_doc_id = '9', page_number = '1', created_at = '2026-06-05 02:04:48.268157+00' WHERE id = 22;
UPDATE extracted_metrics SET metric_name = 'TOTAL NON-CURRENT LIABILITIES', raw_value = '3,572,467', unit = NULL, source_doc_id = '9', page_number = '1', created_at = '2026-06-05 02:04:48.268157+00' WHERE id = 23;
UPDATE extracted_metrics SET metric_name = 'Trade and other payables', raw_value = '1,211,133', unit = NULL, source_doc_id = '9', page_number = '1', created_at = '2026-06-05 02:04:48.268157+00' WHERE id = 24;
UPDATE extracted_metrics SET metric_name = 'Borrowings', raw_value = '110,113', unit = NULL, source_doc_id = '9', page_number = '1', created_at = '2026-06-05 02:04:48.268157+00' WHERE id = 25;
UPDATE extracted_metrics SET metric_name = 'TOTAL CURRENT LIABILITIES', raw_value = '1,321,246', unit = NULL, source_doc_id = '9', page_number = '1', created_at = '2026-06-05 02:04:48.268157+00' WHERE id = 26;
DELETE FROM deals WHERE deal_id = '70569c56-a6b5-40dd-a2cf-ecbb611b5b9b';
COMMIT;