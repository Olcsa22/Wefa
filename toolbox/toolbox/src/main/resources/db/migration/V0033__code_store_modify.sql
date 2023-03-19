INSERT INTO code_store_item (id, code_store_type_id, caption, created_by, created_on, modified_by, modified_on)
VALUES (420, 4, '{"hu":"félkész", "en":"planned"}', 2, NOW(), 2, NOW());

--

ALTER TABLE remote_file
ALTER COLUMN public_url TYPE VARCHAR(1000);