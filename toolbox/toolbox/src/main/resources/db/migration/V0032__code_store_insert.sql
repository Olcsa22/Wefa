UPDATE code_store_item SET caption = '{"hu":"távoli tárhely szolgáltató státusz", "en":"remote provider status"}' WHERE id = 12;

UPDATE code_store_item SET caption = '{"hu":"távoli tárhely szolgáltató prioritás", "en":"remote provider priority"}' WHERE id = 13;

--

INSERT INTO code_store_type (id, caption, created_by, created_on, modified_by, modified_on)
VALUES (14, '{"hu":"távoli tárhely szolgáltató típusa", "en":"remote provider type"}', 2, NOW(), 2, NOW());

INSERT INTO code_store_item (id, code_store_type_id, caption, created_by, created_on, modified_by, modified_on)
VALUES (1400, 14, '{"hu":"google drive", "en":"google drive"}', 2, NOW(), 2, NOW());

INSERT INTO code_store_item (id, code_store_type_id, caption, created_by, created_on, modified_by, modified_on)
VALUES (1402, 14, '{"hu":"amazon s3", "en":"amazon s3"}', 2, NOW(), 2, NOW());