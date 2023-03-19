/**CODE STORE TYPES **/

INSERT INTO code_store_type (id, caption, created_by, created_on, modified_by, modified_on)
VALUES (12, '{"hu":"google drive státusz", "en":"google drive status"}', 2, NOW(), 2, NOW());

INSERT INTO code_store_type (id, caption, created_by, created_on, modified_by, modified_on)
VALUES (13, '{"hu":"google drive prioritás", "en":"google drive priority"}', 2, NOW(), 2, NOW());

/** CODE STORE ITEMS **/

/** STATUS **/
INSERT INTO code_store_item (id, code_store_type_id, caption, created_by, created_on, modified_by, modified_on)
VALUES (1200, 12, '{"hu":"folyamatban", "en":"pending"}', 2, NOW(), 2, NOW());

INSERT INTO code_store_item (id, code_store_type_id, caption, created_by, created_on, modified_by, modified_on)
VALUES (1202, 12, '{"hu":"feltöltve", "en":"uploaded"}', 2, NOW(), 2, NOW());

INSERT INTO code_store_item (id, code_store_type_id, caption, created_by, created_on, modified_by, modified_on)
VALUES (1204, 12, '{"hu":"hiba", "en":"failed"}', 2, NOW(), 2, NOW());

/** PRIORITY **/
INSERT INTO code_store_item (id, code_store_type_id, caption, created_by, created_on, modified_by, modified_on)
VALUES (1300, 13, '{"hu":"alacsony", "en":"low"}', 2, NOW(), 2, NOW());

INSERT INTO code_store_item (id, code_store_type_id, caption, created_by, created_on, modified_by, modified_on)
VALUES (1302, 13, '{"hu":"közepes", "en":"medium"}', 2, NOW(), 2, NOW());

INSERT INTO code_store_item (id, code_store_type_id, caption, created_by, created_on, modified_by, modified_on)
VALUES (1304, 13, '{"hu":"magas", "en":"high"}', 2, NOW(), 2, NOW());

INSERT INTO code_store_item (id, code_store_type_id, caption, created_by, created_on, modified_by, modified_on)
VALUES (1306, 13, '{"hu":"nagyon magas", "en":"very high"}', 2, NOW(), 2, NOW());