ALTER TABLE file_descriptor ADD COLUMN locked_by INT REFERENCES auth_user(id);
ALTER TABLE file_descriptor ADD COLUMN locked_on TIMESTAMP;