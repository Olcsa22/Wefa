CREATE OR REPLACE FUNCTION create_tenant(tenant_name text, email text, password text, phone text DEFAULT NULL)
RETURNS void AS $$

DECLARE

	currentTenantId tenant.id%TYPE;
	adminUserId auth_user.id%TYPE;

BEGIN
	
	-- projekt specifikusan felulirhato
	-- plusz lasd meg _test valtozat
	-- plusz lasd meg TenantService Java lehetosegei

	INSERT INTO tenant (name, created_by, created_on, modified_by, modified_on)
	VALUES (tenant_name, 2, NOW(), 2, NOW());

	SELECT currval('tenant_id_seq') INTO currentTenantId;

	INSERT INTO tenant_info(tenant_id, email, phone, created_by, created_on, modified_by, modified_on)
	VALUES (currentTenantId, email, phone, 2, NOW(), 2, NOW());
	
	INSERT INTO tenant_key_value_settings (tenant_id, kv_key, kv_value, created_by, created_on, modified_by, modified_on)
	VALUES (currentTenantId, 'preferred_currency', null, 2, NOW(), 2, NOW());

	INSERT INTO tenant_key_value_settings (tenant_id, kv_key, kv_value, created_by, created_on, modified_by, modified_on)
	VALUES (currentTenantId, 'preferred_locale', null, 2, NOW(), 2, NOW());

	INSERT INTO tenant_key_value_settings (tenant_id, kv_key, kv_value, created_by, created_on, modified_by, modified_on)
	VALUES (currentTenantId, 'preferred_time_zone', null, 2, NOW(), 2, NOW());

	INSERT INTO tenant_key_value_settings (tenant_id, kv_key, kv_value, created_by, created_on, modified_by, modified_on)
	VALUES (currentTenantId, 'preferred_theme', null, 2, NOW(), 2, NOW());

	INSERT INTO tenant_key_value_settings (tenant_id, kv_key, kv_value, created_by, created_on, modified_by, modified_on)
	VALUES (currentTenantId, 'preferred_measurement_system', null, 2, NOW(), 2, NOW());

	INSERT INTO auth_user (tenant_id, username, enabled, user_roles, created_by, created_on, modified_by, modified_on)
	VALUES (currentTenantId, 'admin', true, '[100, 102, 106, 110]', 2, NOW(), 2, NOW());

	SELECT currval('auth_user_id_seq') INTO adminUserId;

	INSERT INTO auth_user_info (tenant_id, user_id, given_name, family_name, email, created_by, created_on, modified_by, modified_on)
	VALUES (currentTenantId, adminUserId, 'Admin', 'Admin', email, 2, NOW(), 2, NOW());
	
	INSERT INTO auth_user_password (tenant_id, user_id, password)
	VALUES (currentTenantId, adminUserId, password);

END;
$$
LANGUAGE plpgsql;