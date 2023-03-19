CREATE TABLE email (
	tenant_id INT NOT NULL REFERENCES tenant(id),
	id SERIAL PRIMARY KEY,
	from_email VARCHAR(255),
	to_email VARCHAR(255) NOT NULL,
	subject VARCHAR(255) NOT NULL,
	body TEXT NOT NULL,
	is_plain_text BOOLEAN NOT NULL DEFAULT FALSE,
	status INT NOT NULL REFERENCES code_store_item(id),
	attempt INT NOT NULL,
	error_message TEXT,
	created_by INT NOT NULL REFERENCES auth_user,
	created_on TIMESTAMP NOT NULL,
	modified_by INT NOT NULL REFERENCES auth_user,
	modified_on TIMESTAMP NOT NULL
);

CREATE INDEX ON email (tenant_id);
CREATE INDEX ON email (status);
CREATE INDEX ON email (from_email);
CREATE INDEX ON email (to_email);