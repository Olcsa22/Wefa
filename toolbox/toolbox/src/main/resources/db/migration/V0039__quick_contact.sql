CREATE TABLE quick_contact (
  tenant_id INT NOT NULL REFERENCES tenant(id),
	id SERIAL PRIMARY KEY,
  partner_name VARCHAR(50),
  contact_name VARCHAR(50),
  category VARCHAR(50),
  phone_number VARCHAR(50),
  country VARCHAR(100),
  city VARCHAR(50),
  city_details VARCHAR(100),
  email VARCHAR(100),
  note VARCHAR(500),
  origin VARCHAR(100),
	created_by INT NOT NULL REFERENCES auth_user,
	created_on TIMESTAMP NOT NULL,
	modified_by INT NOT NULL REFERENCES auth_user,
	modified_on TIMESTAMP NOT NULL
);