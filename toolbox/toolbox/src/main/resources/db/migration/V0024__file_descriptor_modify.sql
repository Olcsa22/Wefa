ALTER TABLE file_descriptor
ADD COLUMN meta_3_type INT REFERENCES code_store_type(id)