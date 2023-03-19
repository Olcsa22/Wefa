ALTER TABLE payment_transaction DROP COLUMN group_gid;
ALTER TABLE payment_transaction DROP COLUMN approximate_amount_in_usd;
ALTER TABLE payment_transaction DROP COLUMN status_check_frequency;

ALTER TABLE payment_transaction ADD COLUMN lcu_gid VARCHAR(50);

ALTER TABLE payment_transaction ADD COLUMN customer_email VARCHAR(100);
ALTER TABLE payment_transaction ADD COLUMN order_details JSONB;
ALTER TABLE payment_transaction ADD COLUMN meta_1 VARCHAR(200);
ALTER TABLE payment_transaction ADD COLUMN meta_2 VARCHAR(200);