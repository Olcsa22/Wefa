ALTER TABLE holiday RENAME COLUMN dstart TO event_date;
ALTER TABLE holiday ADD COLUMN event_country VARCHAR(100);