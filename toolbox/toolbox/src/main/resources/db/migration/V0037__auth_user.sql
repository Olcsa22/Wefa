INSERT INTO code_store_item (id, code_store_type_id, caption, command, created_by, created_on, modified_by, modified_on)
VALUES (114, 1, '{"hu":"super remote jogosultság", "en":"super remote role"}', 'ROLE_SUPER_REMOTE', 2, NOW(), 2, NOW());

UPDATE auth_user SET user_roles = '[100, 102, 106, 110, 112, 114]' WHERE tenant_id = 1 AND username = 'remote'