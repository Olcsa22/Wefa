ALTER TABLE auth_user ADD COLUMN parent_id INT REFERENCES auth_user(id);

DELETE FROM auth_user_key_value_settings WHERE tenant_id = 1 AND user_id = 1;