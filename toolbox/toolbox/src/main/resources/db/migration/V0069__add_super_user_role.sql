INSERT INTO code_store_item (id, code_store_type_id, caption, command, created_by, created_on, modified_by, modified_on)
VALUES (120, 1, '{"hu":"super felhasználói jogosultság", "en":"super user role"}', 'ROLE_SUPER_USER', 2, NOW(), 2, NOW());

UPDATE auth_user SET user_roles = '[100,102,104,106,110,116,118,120]' WHERE tenant_id = 1 AND id = 1;