INSERT INTO code_store_item (id, code_store_type_id, caption, command, created_by, created_on, modified_by, modified_on)
VALUES (118, 1, '{"hu":"super felügyelő jogosultság", "en":"super tenant overseer role"}', 'ROLE_SUPER_TENANT_OVERSEER', 2, NOW(), 2, NOW());

UPDATE auth_user SET user_roles = '[100,102,104,106,110,116,118]' WHERE tenant_id = 1 AND id = 1;