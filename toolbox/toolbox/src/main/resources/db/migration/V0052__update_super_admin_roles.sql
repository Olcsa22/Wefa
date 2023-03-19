UPDATE auth_user SET user_roles = '[100,102,104,106,110,116]' WHERE tenant_id = 1 AND id = 1;

INSERT INTO auth_user_key_value_settings (tenant_id, user_id, kv_key, kv_value, created_by, created_on, modified_by, modified_on, manual_edit_allowed)
VALUES (1, 1, 'allowed_tenants', '[0]', 2, NOW(), 2, NOW(), true);