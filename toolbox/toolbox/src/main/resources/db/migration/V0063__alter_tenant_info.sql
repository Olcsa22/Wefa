ALTER TABLE tenant_info ADD COLUMN company_tax_number VARCHAR(50);
ALTER TABLE tenant_info ADD COLUMN company_registration_number VARCHAR(50);
ALTER TABLE tenant_info ADD COLUMN company_bank_account_number VARCHAR(50);

ALTER TABLE tenant_info ADD COLUMN company_address_country CHAR(2);
ALTER TABLE tenant_info ADD COLUMN company_address_state VARCHAR(50);
ALTER TABLE tenant_info ADD COLUMN company_address_county VARCHAR(50);
ALTER TABLE tenant_info ADD COLUMN company_address_zip_code VARCHAR(10);
ALTER TABLE tenant_info ADD COLUMN company_address_details VARCHAR(50);

ALTER TABLE tenant_info ADD COLUMN contact_name VARCHAR (50);

ALTER TABLE tenant_info ADD COLUMN note VARCHAR(500);

ALTER TABLE tenant_info ADD COLUMN extra_data_1 VARCHAR(100);
ALTER TABLE tenant_info ADD COLUMN extra_data_2 VARCHAR(100);
ALTER TABLE tenant_info ADD COLUMN extra_data_3 VARCHAR(100);