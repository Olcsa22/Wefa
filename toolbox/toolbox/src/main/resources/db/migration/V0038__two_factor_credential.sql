-- google authentication-hoz szukseges tabla
CREATE TABLE TWO_FACTOR_CREDENTIAL (
	id SERIAL PRIMARY KEY,
	tenant_id INT NOT NULL REFERENCES TENANT(id),
	username VARCHAR(100) NOT NULL,
	secret_code VARCHAR(100) NOT NULL,
	validation_code INT NOT NULL,
	scratch_codes INTEGER[],
	created_by INTEGER NOT NULL REFERENCES AUTH_USER(id),
	created_on TIMESTAMP NOT NULL,
	modified_by INTEGER NOT NULL REFERENCES AUTH_USER(id),
	modified_on TIMESTAMP NOT NULL
);

ALTER TABLE auth_user_password ADD COLUMN two_factor_type INT REFERENCES CODE_STORE_ITEM(id);
ALTER TABLE auth_user_password ADD COLUMN two_factor_enabled BOOLEAN;
ALTER TABLE auth_user_password ADD COLUMN two_factor_value JSONB;

INSERT INTO code_store_type (id, caption, created_by, created_on, modified_by, modified_on, expandable)
VALUES (15, '{"hu":"kétfaktoros azonosítás", "en":"two factor authentication"}', 2, NOW(), 2, NOW(), TRUE);

INSERT INTO code_store_item (id, code_store_type_id, caption, created_by, created_on, modified_by, modified_on)
VALUES (1500, 15, '{"hu":"google", "en":"google"}', 2, NOW(), 2, NOW());