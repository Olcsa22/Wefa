INSERT INTO code_store_item (id, code_store_type_id, caption, command, created_by, created_on, modified_by, modified_on)
VALUES (112, 1, '{"hu":"remote", "en":"remote"}', 'ROLE_REMOTE', 2, NOW(), 2, NOW());

INSERT INTO auth_user (tenant_id, id, username, enabled, user_roles, created_by, created_on, modified_by, modified_on)
VALUES (1, 4, 'remote', true, '[100, 102, 106, 108, 110, 112]', 2, NOW(), 2, NOW());

INSERT INTO auth_user_password (tenant_id, id, user_id, password)
VALUES (1, 4, 4, '$2a$12$XEmDBxKAdSwQHN0aHLoqk.pexvwH8ZLW9Za3J06vIcWlNINSegojm'); --"raGeDREJaspew6" ugyanaz, mint a common-tenant/admin alap jelszava (lasd meg properties fajl)

INSERT INTO auth_user_info (tenant_id, id, user_id, given_name, family_name, email, created_by, created_on, modified_by, modified_on)
VALUES (1, 4, 4, 'Remote', 'Remote', 'remote@example.com', 2, NOW(), 2, NOW());