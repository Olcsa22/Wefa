ALTER TABLE tenant_info ADD COLUMN extra_data_4 INT REFERENCES code_store_item(id);
ALTER TABLE tenant_info ADD COLUMN extra_data_5 JSONB;
ALTER TABLE tenant_info ADD COLUMN extra_data_6 JSONB;