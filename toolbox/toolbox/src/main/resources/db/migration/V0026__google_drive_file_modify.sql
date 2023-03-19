ALTER TABLE google_drive_file 
ADD CONSTRAINT priority_fkey FOREIGN KEY (priority) REFERENCES code_store_item (id);

ALTER TABLE google_drive_file
ALTER COLUMN web_view_url DROP NOT NULL;

ALTER TABLE google_drive_file
ADD attempt INT DEFAULT (0)