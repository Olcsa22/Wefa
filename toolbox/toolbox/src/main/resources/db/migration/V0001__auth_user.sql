CREATE TABLE auth_user (
	tenant_id INT NOT NULL REFERENCES tenant(id),
	id SERIAL PRIMARY KEY,
	username VARCHAR(100) NOT NULL,
	superior INT REFERENCES auth_user(id),
	note VARCHAR(160),
	enabled BOOLEAN NOT NULL DEFAULT FALSE,
	user_roles JSONB NOT NULL DEFAULT '[]',
	anonymized_deleted BOOLEAN DEFAULT FALSE,
	additional_data1 VARCHAR(50),
	additional_data2 VARCHAR(50),
	created_by INT NOT NULL REFERENCES auth_user,
	created_on TIMESTAMP NOT NULL,
	modified_by INT NOT NULL REFERENCES auth_user,
	modified_on TIMESTAMP NOT NULL,
	UNIQUE(tenant_id, username) DEFERRABLE INITIALLY IMMEDIATE
);

INSERT INTO auth_user (tenant_id, id, username, enabled, user_roles, created_by, created_on, modified_by, modified_on)
VALUES (1, 1, 'admin', true, '[100, 102, 104, 106, 108, 110]', 1, NOW(), 1, NOW());

INSERT INTO auth_user (tenant_id, id, username, enabled, user_roles, created_by, created_on, modified_by, modified_on)
VALUES (1, 2, 'system', true, '[]', 2, NOW(), 2, NOW());

INSERT INTO auth_user (tenant_id, id, username, enabled, user_roles, created_by, created_on, modified_by, modified_on)
VALUES (1, 3, 'anonymous', true, '[]', 2, NOW(), 2, NOW());

SELECT SETVAL('auth_user_id_seq', 10);

CREATE TABLE auth_user_password (
	tenant_id INT NOT NULL REFERENCES tenant(id),
	id SERIAL PRIMARY KEY,
	user_id INT NOT NULL REFERENCES auth_user(id),
	password VARCHAR(100) NOT NULL,
	UNIQUE(user_id) DEFERRABLE INITIALLY IMMEDIATE
);

CREATE INDEX ON auth_user_password (tenant_id);

INSERT INTO auth_user_password (tenant_id, id, user_id, password)
VALUES (1, 1, 1, '$2a$12$XEmDBxKAdSwQHN0aHLoqk.pexvwH8ZLW9Za3J06vIcWlNINSegojm');

SELECT SETVAL('auth_user_password_id_seq', 10);

CREATE TABLE auth_user_sso (
	id SERIAL PRIMARY KEY,
	user_id INT NOT NULL REFERENCES auth_user(id),
	sso_id VARCHAR(100) NOT NULL,
	sso_type VARCHAR(50),
	UNIQUE(user_id) DEFERRABLE INITIALLY IMMEDIATE
);

CREATE UNIQUE INDEX ON auth_user_sso (LOWER(sso_id));

CREATE TABLE auth_user_info (
	tenant_id INT NOT NULL REFERENCES tenant(id),
	id SERIAL PRIMARY KEY,
	user_id INT NOT NULL REFERENCES auth_user(id),
	date_of_birth DATE,
	email VARCHAR(100) NOT NULL,
	title VARCHAR(50),
	family_name VARCHAR(100),
	given_name VARCHAR(100),
	job_title VARCHAR(50),
	phone_number VARCHAR(100),
	created_by INT NOT NULL REFERENCES auth_user,
	created_on TIMESTAMP NOT NULL,
	modified_by INT NOT NULL REFERENCES auth_user,
	modified_on TIMESTAMP NOT NULL,
	UNIQUE(user_id) DEFERRABLE INITIALLY IMMEDIATE
);

INSERT INTO auth_user_info (tenant_id, id, user_id, given_name, family_name, email, created_by, created_on, modified_by, modified_on)
VALUES (1, 1, 1, 'Admin', 'Super', 'super-admin@example.com', 1, NOW(), 1, NOW());

INSERT INTO auth_user_info (tenant_id, id, user_id, given_name, family_name, email, created_by, created_on, modified_by, modified_on)
VALUES (1, 2, 2, 'System', 'System', 'system@example.com', 2, NOW(), 2, NOW());

SELECT SETVAL('auth_user_info_id_seq', 10);

CREATE UNIQUE INDEX ON auth_user_info (user_id);
CREATE INDEX ON auth_user_info (lower(given_name));
CREATE INDEX ON auth_user_info (lower(family_name));
CREATE INDEX ON auth_user_info (lower(email));

CREATE TABLE auth_user_key_value_settings (
	tenant_id INT NOT NULL REFERENCES tenant(id),
	id SERIAL PRIMARY KEY,
	user_id INT NOT NULL REFERENCES auth_user(id),
	key VARCHAR(50) NOT NULL,
	value VARCHAR(50) NOT NULL,
	created_by INT NOT NULL REFERENCES auth_user,
	created_on TIMESTAMP NOT NULL,
	modified_by INT NOT NULL REFERENCES auth_user,
	modified_on TIMESTAMP NOT NULL
);

CREATE UNIQUE INDEX ON auth_user_key_value_settings (user_id, lower(key));