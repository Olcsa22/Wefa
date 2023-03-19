CREATE TABLE auth_token (
	tenant_id INT NOT NULL REFERENCES tenant(id),
	id SERIAL PRIMARY KEY,
 	token_type INT NOT NULL REFERENCES code_store_item(id),
	token VARCHAR(100) NOT NULL,
	valid_until TIMESTAMP NOT NULL,
	resource_id1 INT NOT NULL,
	resource_id2 INT,
	note1 VARCHAR(200),
	note2 VARCHAR(200),
	note3 VARCHAR(200),
	created_by INT NOT NULL REFERENCES auth_user,
	created_on TIMESTAMP NOT NULL,
	modified_by INT NOT NULL REFERENCES auth_user,
	modified_on TIMESTAMP NOT NULL
);

CREATE INDEX ON auth_token (tenant_id);
CREATE INDEX ON auth_token (token_type);
CREATE UNIQUE INDEX ON auth_token (lower(token)); -- itt globálisan kell unique legyen a token