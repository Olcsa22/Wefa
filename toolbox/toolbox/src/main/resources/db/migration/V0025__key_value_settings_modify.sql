ALTER TABLE tenant_key_value_settings
RENAME COLUMN "key" TO kv_key;

ALTER TABLE tenant_key_value_settings
RENAME COLUMN "value" TO kv_value;

ALTER TABLE auth_user_key_value_settings
RENAME COLUMN "key" TO kv_key;

ALTER TABLE auth_user_key_value_settings
RENAME COLUMN "value" TO kv_value;

/* ------------------ */

ALTER TABLE tenant_key_value_settings
ALTER COLUMN kv_value TYPE VARCHAR(100);

ALTER TABLE auth_user_key_value_settings
ALTER COLUMN kv_value TYPE VARCHAR(100);

/* ------------------ */

ALTER TABLE tenant_key_value_settings
ADD COLUMN kv_long_text TEXT;

ALTER TABLE auth_user_key_value_settings
ADD COLUMN kv_long_text TEXT;

/* ------------------ */

ALTER TABLE tenant_key_value_settings
ALTER COLUMN kv_value DROP NOT NULL;

ALTER TABLE auth_user_key_value_settings
ALTER COLUMN kv_value DROP NOT NULL;

/* ------------------ */

ALTER TABLE tenant_key_value_settings
ADD COLUMN manual_edit_allowed BOOLEAN NOT NULL DEFAULT TRUE;

ALTER TABLE auth_user_key_value_settings
ADD COLUMN manual_edit_allowed BOOLEAN NOT NULL DEFAULT TRUE;