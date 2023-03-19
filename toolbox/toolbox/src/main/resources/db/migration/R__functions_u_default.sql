CREATE OR REPLACE FUNCTION user_delete_anonimizer(userId int)
RETURNS void AS $$

BEGIN
	DELETE FROM auth_user_info WHERE user_id = userId;

	DELETE FROM auth_user_password WHERE user_id = userId;

	UPDATE auth_user SET
	username = 'deleted_user_' || (SELECT md5(random()::text || clock_timestamp()::text)::uuid),
	superior = null,
	note = null,
	enabled = false,
	locale = 'hu_HU',
	time_zone = 'CET',
	user_roles = '[]',
	anonymized_deleted = TRUE,
	additional_data1 = null,
	additional_data2 = null,
	created_by = 2,
	created_on = '2000-01-01 11:01:01.079336',
	modified_by = 2,
	modified_on = '2000-01-01 11:01:01.079336',
	profile_img = null
	WHERE auth_user.id = userId;

END;
$$
LANGUAGE plpgsql;