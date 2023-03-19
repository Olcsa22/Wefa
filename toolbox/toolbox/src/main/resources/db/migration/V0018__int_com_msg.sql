CREATE TABLE INT_COM_MSG (
	tenant_id INT NOT NULL REFERENCES tenant(id),
	id SERIAL PRIMARY KEY,
	subject VARCHAR(255) NOT NULL,
	body TEXT NOT NULL,
	meta_data JSONB,
	todo_marker BOOLEAN NOT NULL,
	automatic_msg BOOLEAN NOT NULL,
	created_by INT NOT NULL REFERENCES auth_user(id),
	created_on TIMESTAMP NOT NULL,
	modified_by INT NOT NULL REFERENCES auth_user(id),
	modified_on TIMESTAMP NOT NULL
);

CREATE TABLE INT_COM_MSG_X_USER (
	tenant_id INT NOT NULL REFERENCES tenant(id),
	id SERIAL PRIMARY KEY,
	user_id INT NOT NULL REFERENCES auth_user(id),
	msg_id INT NOT NULL REFERENCES int_com_msg(id),
	seen_on TIMESTAMP,
	is_hidden BOOLEAN DEFAULT FALSE,
	created_by INT NOT NULL REFERENCES auth_user(id),
	created_on TIMESTAMP NOT NULL,
	modified_by INT NOT NULL REFERENCES auth_user(id),
	modified_on TIMESTAMP NOT NULL
);