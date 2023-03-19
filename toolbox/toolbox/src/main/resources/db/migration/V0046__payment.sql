
CREATE TABLE payment_config (
	
	tenant_id INT NOT NULL REFERENCES tenant(id),
	id SERIAL PRIMARY KEY,

	payment_provider INT NOT NULL REFERENCES code_store_item(id),
	config_json JSONB,
	file_ids JSONB,
	
	use_global_config BOOLEAN DEFAULT FALSE,
	
	enabled BOOLEAN NOT NULL DEFAULT FALSE,
	
	created_by INT NOT NULL REFERENCES auth_user,
	created_on TIMESTAMP NOT NULL,
	modified_by INT NOT NULL REFERENCES auth_user,
	modified_on TIMESTAMP NOT NULL,
	
	CONSTRAINT payment_config_payment_provider_check CHECK (((payment_provider >= 1800) AND (payment_provider < 1900))),
	CONSTRAINT payment_config_global_config_check CHECK (((use_global_config = TRUE AND config_json IS NULL AND file_ids IS NULL) OR (use_global_config = FALSE AND (config_json IS NOT NULL OR file_ids IS NOT NULL))))
);

CREATE TABLE payment_transaction (

	tenant_id INT NOT NULL REFERENCES tenant(id),
	
	id SERIAL PRIMARY KEY,
	gid VARCHAR(150) NOT NULL,
	group_gid VARCHAR(150),
	referred_transaction_id INT REFERENCES payment_transaction(id),
	
	raw_id VARCHAR(500),

	payment_provider INT NOT NULL REFERENCES code_store_item(id),
	payment_operation_type INT NOT NULL REFERENCES code_store_item(id),
	
	status INT NOT NULL REFERENCES code_store_item(id),

	original_currency VARCHAR(3) NOT NULL,
	original_amount DECIMAL(19,2) NOT NULL,

	approximate_amount_in_usd DECIMAL(19,2),

	order_id VARCHAR(150) NOT NULL,
	
	customer_user_id INT REFERENCES auth_user(id),
	customer_id VARCHAR(150) NOT NULL,

	provider_specific_input JSONB,
	provider_specific_processing_data JSONB,

	back_office_notes VARCHAR(500),
   
	order_info_label VARCHAR(75),
   
	file_ids JSONB,

	payment_provider_confirm_ui_url VARCHAR(1000),
	return_url VARCHAR(1000),
	
	last_status_check_request_on TIMESTAMP,

	status_check_frequency INT NOT NULL REFERENCES code_store_item(id),

	last_status_check_reserved_for VARCHAR(100),

	additional_deposit_stage_count INT,
	additional_deposit_stage_finished INT,

	manual_status_change BOOLEAN,

	created_by INT NOT NULL REFERENCES auth_user,
	created_on TIMESTAMP NOT NULL,
	modified_by INT NOT NULL REFERENCES auth_user,
	modified_on TIMESTAMP NOT NULL,

	CHECK (((last_status_check_request_on IS NULL) AND (last_status_check_reserved_for IS NULL))
	OR ((last_status_check_request_on IS NOT NULL) AND (last_status_check_reserved_for IS NOT NULL))),

	CONSTRAINT payment_transaction_payment_provider_check CHECK (((payment_provider >= 1800) AND (payment_provider < 1900))),
	CONSTRAINT payment_transaction_payment_operation_type_check CHECK (((payment_operation_type >= 1600) AND (payment_operation_type < 1700))),
	CONSTRAINT payment_transaction_status_check CHECK (((status >= 1700) AND (status < 1800))),
	CONSTRAINT payment_transaction_status_check_frequency_check CHECK (((status_check_frequency >= 1900) AND (status_check_frequency < 2000)))
);

---

CREATE TABLE payment_transaction_raw_log (
	
	tenant_id INT NOT NULL REFERENCES tenant(id),
	id SERIAL PRIMARY KEY,

	payment_transaction_id INT NOT NULL REFERENCES payment_transaction(id),

	log_data VARCHAR(2000),
	
	created_by INT NOT NULL REFERENCES auth_user,
	created_on TIMESTAMP NOT NULL,
	modified_by INT NOT NULL REFERENCES auth_user,
	modified_on TIMESTAMP NOT NULL
);

---

INSERT INTO code_store_type (id, caption, created_by, created_on, modified_by, modified_on)
VALUES (16, '{"hu":"fizetési tranzakció típus", "en":"payment operation type"}', 2, NOW(), 2, NOW());

INSERT INTO code_store_item (id, code_store_type_id, caption, command, created_by, created_on, modified_by, modified_on)
VALUES (1600, 16, '{"en":"auth"}', 'AUTH', 2, NOW(), 2, NOW());
INSERT INTO code_store_item (id, code_store_type_id, caption, command, created_by, created_on, modified_by, modified_on)
VALUES (1602, 16, '{"en":"purchase"}', 'PURCHASE', 2, NOW(), 2, NOW());
INSERT INTO code_store_item (id, code_store_type_id, caption, command, created_by, created_on, modified_by, modified_on)
VALUES (1604, 16, '{"en":"capture"}', 'CAPTURE', 2, NOW(), 2, NOW());
INSERT INTO code_store_item (id, code_store_type_id, caption, command, created_by, created_on, modified_by, modified_on)
VALUES (1606, 16, '{"en":"refund"}', 'REFUND', 2, NOW(), 2, NOW());
INSERT INTO code_store_item (id, code_store_type_id, caption, command, created_by, created_on, modified_by, modified_on)
VALUES (1608, 16, '{"en":"void"}', 'VOID', 2, NOW(), 2, NOW());
INSERT INTO code_store_item (id, code_store_type_id, caption, command, created_by, created_on, modified_by, modified_on)
VALUES (1610, 16, '{"en":"withdraw"}', 'WITHDRAW', 2, NOW(), 2, NOW());

---

INSERT INTO code_store_type (id, caption, created_by, created_on, modified_by, modified_on)
VALUES (17, '{"hu":"fizetési tranzakció státusza", "en":"payment transaction status"}', 2, NOW(), 2, NOW());

INSERT INTO code_store_item (id, code_store_type_id, caption, command, created_by, created_on, modified_by, modified_on)
VALUES (1704, 17, '{"hu":"gateway inicializáció", "en":"gateway init"}', 'GATEWAY_INIT', 2, NOW(), 2, NOW());
INSERT INTO code_store_item (id, code_store_type_id, caption, command, created_by, created_on, modified_by, modified_on)
VALUES (1720, 17, '{"hu":"függőben", "en":"pending"}', 'PENDING', 2, NOW(), 2, NOW());
INSERT INTO code_store_item (id, code_store_type_id, caption, command, created_by, created_on, modified_by, modified_on)
VALUES (1730, 17, '{"hu":"fogadott", "en":"received"}', 'RECEIVED', 2, NOW(), 2, NOW());
INSERT INTO code_store_item (id, code_store_type_id, caption, command, created_by, created_on, modified_by, modified_on)
VALUES (1740, 17, '{"hu":"sikertelen", "en":"failed"}', 'FAILED', 2, NOW(), 2, NOW());
INSERT INTO code_store_item (id, code_store_type_id, caption, command, created_by, created_on, modified_by, modified_on)
VALUES (1742, 17, '{"hu":"vásárló megszakította", "en":"customer canceled"}', 'CANCELED', 2, NOW(), 2, NOW());
INSERT INTO code_store_item (id, code_store_type_id, caption, command, created_by, created_on, modified_by, modified_on)
VALUES (1750, 17, '{"hu":"sikeres", "en":"success"}', 'SUCCESS', 2, NOW(), 2, NOW());


---

INSERT INTO code_store_type (id, caption, created_by, created_on, modified_by, modified_on)
VALUES (18, '{"hu":"fizetési szolgáltató", "en":"payment provider"}', 2, NOW(), 2, NOW());

INSERT INTO code_store_item (id, code_store_type_id, caption, command, created_by, created_on, modified_by, modified_on)
VALUES (1802, 18, '{"en":"Dummy (test)", "hu":"Dummy (teszt)"}', 'DUMMY', 2, NOW(), 2, NOW());

INSERT INTO code_store_item (id, code_store_type_id, caption, command, created_by, created_on, modified_by, modified_on)
VALUES (1810, 18, '{"en":"OTP"}', 'OTP', 2, NOW(), 2, NOW());
INSERT INTO code_store_item (id, code_store_type_id, caption, command, created_by, created_on, modified_by, modified_on)
VALUES (1814, 18, '{"en":"SimplePay"}', 'SIMPLEPAY2"', 2, NOW(), 2, NOW());
INSERT INTO code_store_item (id, code_store_type_id, caption, command, created_by, created_on, modified_by, modified_on)
VALUES (1820, 18, '{"en":"Barion"}', 'BARION', 2, NOW(), 2, NOW());
INSERT INTO code_store_item (id, code_store_type_id, caption, command, created_by, created_on, modified_by, modified_on)
VALUES (1830, 18, '{"en":"PayPal"}', 'PAY_PAL', 2, NOW(), 2, NOW());
INSERT INTO code_store_item (id, code_store_type_id, caption, command, created_by, created_on, modified_by, modified_on)
VALUES (1840, 18, '{"en":"Cash, coupon, voucher etc."}', 'IRL', 2, NOW(), 2, NOW());

---

INSERT INTO code_store_type (id, caption, created_by, created_on, modified_by, modified_on)
VALUES (19, '{"hu":"fizetési tranzakció státusz ellenőrzés gyakorisága", "en":"payment transaction status check frequency"}', 2, NOW(), 2, NOW());

INSERT INTO code_store_item (id, code_store_type_id, caption, created_by, created_on, modified_by, modified_on)
VALUES (1900, 19, '{"en":"alpha", "hu":"alfa"}', 2, NOW(), 2, NOW());
INSERT INTO code_store_item (id, code_store_type_id, caption, created_by, created_on, modified_by, modified_on)
VALUES (1902, 19, '{"en":"beta", "hu": "béta"}', 2, NOW(), 2, NOW());
INSERT INTO code_store_item (id, code_store_type_id, caption, created_by, created_on, modified_by, modified_on)
VALUES (1904, 19, '{"en":"gamma", "hu": "gamma"}', 2, NOW(), 2, NOW());
