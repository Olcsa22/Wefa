ALTER TABLE tenant_info ADD COLUMN extra_data_7 JSONB;

UPDATE code_store_type SET expandable = false WHERE id = 15;