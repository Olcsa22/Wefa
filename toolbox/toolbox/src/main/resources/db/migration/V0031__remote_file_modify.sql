ALTER TABLE remote_file RENAME COLUMN web_view_url TO public_url;
ALTER TABLE remote_file RENAME COLUMN google_drive_id TO remote_id;
ALTER TABLE remote_file ADD COLUMN remote_provider_type INT NOT NULL REFERENCES code_store_item(id);