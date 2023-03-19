CREATE TABLE persistent_login (
	series VARCHAR(64) PRIMARY KEY NOT NULL, 
	token_value VARCHAR(64) NOT NULL,
	date TIMESTAMP NOT NULL,
	username VARCHAR(100) NOT NULL -- REFERENCES auth_user(username)
);

CREATE INDEX ON persistent_login (lower(username));