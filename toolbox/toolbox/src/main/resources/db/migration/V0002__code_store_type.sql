CREATE TABLE code_store_type (
	id SERIAL PRIMARY KEY,
	caption JSONB NOT NULL,
	expandable BOOLEAN NOT NULL DEFAULT FALSE,
	enabled BOOLEAN NOT NULL DEFAULT TRUE,
	created_by INT NOT NULL REFERENCES auth_user(id),
	created_on TIMESTAMP NOT NULL,
	modified_by INT NOT NULL REFERENCES auth_user(id),
	modified_on TIMESTAMP NOT NULL
);

INSERT INTO code_store_type (id, caption, created_by, created_on, modified_by, modified_on)
VALUES (1, '{"hu":"felhasználói jogosultság", "en":"user role"}', 2, NOW(), 2, NOW());

INSERT INTO code_store_type (id, caption, created_by, created_on, modified_by, modified_on)
VALUES (2, '{"hu":"auth. token típus", "en":"auth token type"}', 2, NOW(), 2, NOW());

INSERT INTO code_store_type (id, caption, created_by, created_on, modified_by, modified_on)
VALUES (3, '{"hu":"fájlleíró státusza", "en":"file descriptor status"}', 2, NOW(), 2, NOW());

INSERT INTO code_store_type (id, caption, created_by, created_on, modified_by, modified_on)
VALUES (4, '{"hu":"email státusza", "en":"email status"}', 2, NOW(), 2, NOW());

INSERT INTO code_store_type (id, caption, created_by, created_on, modified_by, modified_on)
VALUES (5, '{"hu":"email sablon típusa", "en":"email template type"}', 2, NOW(), 2, NOW());

INSERT INTO code_store_type (id, caption, created_by, created_on, modified_by, modified_on)
VALUES (6, '{"hu":"fájl elhelyezés típusa", "en":"file location type"}', 2, NOW(), 2, NOW());

INSERT INTO code_store_type (id, caption, created_by, created_on, modified_by, modified_on)
VALUES (7, '{"hu":"fájl elérés (security) típusa", "en":"file security type"}', 2, NOW(), 2, NOW());

INSERT INTO code_store_type (id, caption, created_by, created_on, modified_by, modified_on)
VALUES (8, '{"hu":"kontakt adat típusa", "en":"contact address type"}', 2, NOW(), 2, NOW());

INSERT INTO code_store_type (id, caption, created_by, created_on, modified_by, modified_on)
VALUES (9, '{"hu":"földrajzi cím típusa", "en":"geo address type"}', 2, NOW(), 2, NOW());

INSERT INTO code_store_type (id, caption, created_by, created_on, modified_by, modified_on)
VALUES (10, '{"hu":"földrajzi adatok típusa", "en":"geo data type"}', 2, NOW(), 2, NOW());

SELECT SETVAL('code_store_type_id_seq', 500);