INSERT INTO code_store_type (id, caption, created_by, created_on, modified_by, modified_on)
VALUES (11, '{"hu":"export típusa", "en":"export type"}', 2, NOW(), 2, NOW());

INSERT INTO code_store_item (id, code_store_type_id, caption, created_by, created_on, modified_by, modified_on)
VALUES (11000, 11, '{"hu":"XLSX", "en":"XLSX"}', 2, NOW(), 2, NOW());

INSERT INTO code_store_item (id, code_store_type_id, caption, created_by, created_on, modified_by, modified_on)
VALUES (11002, 11, '{"hu":"PDF", "en":"PDF"}', 2, NOW(), 2, NOW());