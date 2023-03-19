ALTER TABLE file_descriptor ADD COLUMN child_type INT REFERENCES code_store_item(id);

INSERT INTO code_store_type (id, caption, created_by, created_on, modified_by, modified_on)
VALUES (20, '{"hu":"fájlleíró gyerek típusa", "en":"file descriptor child type"}', 2, NOW(), 2, NOW());

--
INSERT INTO code_store_item (id, code_store_type_id, caption, created_by, created_on, modified_by, modified_on)
VALUES (2000, 20, '{"en":"2048 2048 PROGR JPG változat", "hu":"Variant 2048 2048 PROGR JPG"}', 2, NOW(), 2, NOW());

INSERT INTO code_store_item (id, code_store_type_id, caption, created_by, created_on, modified_by, modified_on)
VALUES (2002, 20, '{"en":"3096 3096 JPG változat", "hu":"Variant 3096 3096 JPG"}', 2, NOW(), 2, NOW());

INSERT INTO code_store_item (id, code_store_type_id, caption, created_by, created_on, modified_by, modified_on)
VALUES (2004, 20, '{"en":"PDF változat", "hu":"Variant PDF"}', 2, NOW(), 2, NOW());