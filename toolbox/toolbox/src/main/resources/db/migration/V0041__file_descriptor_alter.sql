ALTER TABLE file_descriptor ADD COLUMN remote_only BOOLEAN;
UPDATE file_descriptor SET remote_only = true WHERE status = 308;
DELETE FROM code_store_item WHERE id = 308;