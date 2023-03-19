CREATE TABLE file_descriptor (
	tenant_id INT NOT NULL REFERENCES tenant(id),
	id SERIAL PRIMARY KEY,
	location_type INT NOT NULL REFERENCES code_store_item(id),
	security_type INT NOT NULL REFERENCES code_store_item(id),
	filename VARCHAR(100),
	file_path VARCHAR(100) NOT NULL,
	mime_type VARCHAR(100),
	file_size BIGINT,
	description JSONB,
	hash_value VARCHAR(64),
	gid UUID NOT NULL,
	status INT NOT NULL REFERENCES code_store_item(id),
	created_by INT NOT NULL REFERENCES auth_user(id),
	created_on TIMESTAMP NOT NULL,
	modified_by INT NOT NULL REFERENCES auth_user(id),
	modified_on TIMESTAMP NOT NULL,
	UNIQUE(file_path)
);

CREATE UNIQUE INDEX ON file_descriptor (lower(file_path));
CREATE INDEX ON file_descriptor (tenant_id);
CREATE INDEX ON file_descriptor (gid);
CREATE INDEX ON file_descriptor (lower(mime_type));
CREATE INDEX ON file_descriptor (lower(filename));

ALTER TABLE auth_user ADD COLUMN profile_img INT REFERENCES file_descriptor(id);