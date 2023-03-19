CREATE TABLE tenant (
	id SERIAL PRIMARY KEY,
	name VARCHAR(50) NOT NULL,
	enabled BOOLEAN NOT NULL DEFAULT TRUE,
	anonymized_deleted BOOLEAN DEFAULT FALSE,
	created_by INT,
	created_on TIMESTAMP NOT NULL,
	modified_by INT,
	modified_on TIMESTAMP NOT NULL,
	UNIQUE(name)
);

INSERT INTO tenant(id, name, created_by, created_on, modified_by, modified_on)
VALUES (1, 'common-tenant', NULL, NOW(), NULL , NOW());

INSERT INTO tenant(id, name, created_by, created_on, modified_by, modified_on)
VALUES (2, 'lcu-tenant', NULL, NOW(), NULL , NOW());

SELECT SETVAL('tenant_id_seq', 10);

CREATE TABLE tenant_info (
	id SERIAL PRIMARY KEY,
	tenant_id INT NOT NULL REFERENCES tenant(id),
	email VARCHAR(100) NOT NULL,
	phone VARCHAR(30),
	created_by INT,
	created_on TIMESTAMP NOT NULL,
	modified_by INT,
	modified_on TIMESTAMP NOT NULL,
	UNIQUE(tenant_id) DEFERRABLE INITIALLY IMMEDIATE
);

INSERT INTO tenant_info(id, tenant_id, email, phone, created_by, created_on, modified_by, modified_on)
VALUES (1, 1, 'example@example.com', NULL, NULL, NOW(), NULL , NOW());

INSERT INTO tenant_info(id, tenant_id, email, phone, created_by, created_on, modified_by, modified_on)
VALUES (2, 2, 'example@example.com', NULL, NULL, NOW(), NULL , NOW());

SELECT SETVAL('tenant_info_id_seq', 10);

CREATE INDEX ON tenant_info (email);
CREATE INDEX ON tenant_info (phone);

CREATE TABLE tenant_key_value_settings (
	id SERIAL PRIMARY KEY,
	tenant_id INT NOT NULL REFERENCES tenant(id),
	key VARCHAR(50) NOT NULL,
	value VARCHAR(50) NOT NULL,
	created_by INT,
	created_on TIMESTAMP NOT NULL,
	modified_by INT,
	modified_on TIMESTAMP NOT NULL
);

CREATE UNIQUE INDEX ON tenant_key_value_settings (tenant_id, lower(key));