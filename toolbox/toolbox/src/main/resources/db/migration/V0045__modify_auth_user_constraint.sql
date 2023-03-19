ALTER TABLE auth_user DROP CONSTRAINT auth_user_tenant_id_username_key;
CREATE UNIQUE INDEX auth_user_tenant_id_lower_username_key ON auth_user (tenant_id, LOWER(username));