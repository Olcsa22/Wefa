ALTER TABLE quick_contact RENAME COLUMN meta_field_name1 TO extra_field_name1;
ALTER TABLE quick_contact RENAME COLUMN meta_field_value1 TO extra_field_value1;
ALTER TABLE quick_contact RENAME COLUMN meta_field_name2 TO extra_field_name2;
ALTER TABLE quick_contact RENAME COLUMN meta_field_value2 TO extra_field_value2;
ALTER TABLE quick_contact RENAME COLUMN meta_field_name3 TO extra_field_name3;
ALTER TABLE quick_contact RENAME COLUMN meta_field_value3 TO extra_field_value3;

ALTER TABLE quick_contact DROP COLUMN category;
  
UPDATE quick_contact SET contact_name='-' WHERE contact_name IS NULL;
ALTER TABLE quick_contact ALTER COLUMN contact_name SET NOT NULL;