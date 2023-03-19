UPDATE code_store_item
SET command = 'SIMPLEPAY2'
WHERE id = 1814;

INSERT INTO code_store_item (id, code_store_type_id, caption, command, created_by, created_on, modified_by, modified_on)
VALUES (1835, 18, '{"en":"CIB"}', 'CIB', 2, NOW(), 2, NOW());