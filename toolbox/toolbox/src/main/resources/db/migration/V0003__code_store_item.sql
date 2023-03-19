CREATE TABLE code_store_item (
	id SERIAL PRIMARY KEY,
	code_store_type_id int NOT NULL REFERENCES code_store_type(id),
	caption JSONB NOT NULL,
	command VARCHAR(50),
	enabled BOOLEAN NOT NULL DEFAULT TRUE,
	created_by INT NOT NULL REFERENCES auth_user(id),
	created_on TIMESTAMP NOT NULL,
	modified_by INT NOT NULL REFERENCES auth_user(id),
	modified_on TIMESTAMP NOT NULL
);

CREATE INDEX ON code_store_item (code_store_type_id);

INSERT INTO code_store_item (id, code_store_type_id, caption, command, created_by, created_on, modified_by, modified_on)
VALUES (100, 1, '{"hu":"felhasználói jogosultság", "en":"regular user role"}', 'ROLE_USER', 2, NOW(), 2, NOW());

INSERT INTO code_store_item (id, code_store_type_id, caption, command, created_by, created_on, modified_by, modified_on)
VALUES (102, 1, '{"hu":"admin jogosultság", "en":"admin role"}', 'ROLE_ADMIN', 2, NOW(), 2, NOW());

INSERT INTO code_store_item (id, code_store_type_id, caption, command, created_by, created_on, modified_by, modified_on)
VALUES (104, 1, '{"hu":"super admin jogosultság", "en":"super admin role"}', 'ROLE_SUPER_ADMIN', 2, NOW(), 2, NOW());

INSERT INTO code_store_item (id, code_store_type_id, caption, command, created_by, created_on, modified_by, modified_on)
VALUES (106, 1, '{"hu":"publikus/anonim jogosultság", "en":"public/anonim. role"}', 'ROLE_ANONYMOUS', 2, NOW(), 2, NOW());

INSERT INTO code_store_item (id, code_store_type_id, caption, command, created_by, created_on, modified_by, modified_on)
VALUES (108, 1, '{"hu":"diagnosztikai jogosultság (actuator)", "en":"actuator role"}', 'ACTUATOR', 2, NOW(), 2, NOW());

INSERT INTO code_store_item (id, code_store_type_id, caption, command, created_by, created_on, modified_by, modified_on)
VALUES (110, 1, '{"hu":"lcu", "en":"lcu"}', 'ROLE_LCU', 2, NOW(), 2, NOW());

/* --- */

INSERT INTO code_store_item (id, code_store_type_id, caption, created_by, created_on, modified_by, modified_on)
VALUES (200, 2, '{"hu":"felhasználó regisztráció", "en":"user registration"}', 2, NOW(), 2, NOW());

INSERT INTO code_store_item (id, code_store_type_id, caption, created_by, created_on, modified_by, modified_on)
VALUES (202, 2, '{"hu":"elfelejtett jelszó", "en":"forgotten password"}', 2, NOW(), 2, NOW());

/* --- */

INSERT INTO code_store_item (id, code_store_type_id, caption, created_by, created_on, modified_by, modified_on)
VALUES (304, 3, '{"hu":"átmeneti", "en":"temporary"}', 2, NOW(), 2, NOW());

INSERT INTO code_store_item (id, code_store_type_id, caption, created_by, created_on, modified_by, modified_on)
VALUES (306, 3, '{"hu":"normál", "en":"normal"}', 2, NOW(), 2, NOW());

INSERT INTO code_store_item (id, code_store_type_id, caption, created_by, created_on, modified_by, modified_on)
VALUES (310, 3, '{"hu":"törlendő", "en":"to be deleted"}', 2, NOW(), 2, NOW());

/* --- */

INSERT INTO code_store_item (id, code_store_type_id, caption, created_by, created_on, modified_by, modified_on)
VALUES (400, 4, '{"hu":"létrehozva", "en":"created"}', 2, NOW(), 2, NOW());

INSERT INTO code_store_item (id, code_store_type_id, caption, created_by, created_on, modified_by, modified_on)
VALUES (402, 4, '{"hu":"függőben", "en":"pending"}', 2, NOW(), 2, NOW());

INSERT INTO code_store_item (id, code_store_type_id, caption, created_by, created_on, modified_by, modified_on)
VALUES (404, 4, '{"hu":"hiba", "en":"error"}', 2, NOW(), 2, NOW());

INSERT INTO code_store_item (id, code_store_type_id, caption, created_by, created_on, modified_by, modified_on)
VALUES (410, 4, '{"hu":"elküldve", "en":"sent"}', 2, NOW(), 2, NOW());

/* --- */

INSERT INTO code_store_item (id, code_store_type_id, caption, created_by, created_on, modified_by, modified_on)
VALUES (500, 5, '{"hu":"generált jelszó", "en":"generated password"}', 2, NOW(), 2, NOW());

INSERT INTO code_store_item (id, code_store_type_id, caption, created_by, created_on, modified_by, modified_on)
VALUES (502, 5, '{"hu":"új felhasználó regisztrálása értesítő", "en":"new user registration notification"}', 2, NOW(), 2, NOW());

INSERT INTO code_store_item (id, code_store_type_id, caption, created_by, created_on, modified_by, modified_on)
VALUES (504, 5, '{"hu":"új felhasználó regisztrálásáról értesítő", "en":"new user registered notification"}', 2, NOW(), 2, NOW());

INSERT INTO code_store_item (id, code_store_type_id, caption, created_by, created_on, modified_by, modified_on)
VALUES (506, 5, '{"hu":"elfelejtett jelszó", "en":"forgotten password"}', 2, NOW(), 2, NOW());

/* --- */

INSERT INTO code_store_item (id, code_store_type_id, caption, created_by, created_on, modified_by, modified_on)
VALUES (600, 6, '{"hu":"védett könyvtár", "en":"protected folder"}', 2, NOW(), 2, NOW());

INSERT INTO code_store_item (id, code_store_type_id, caption, created_by, created_on, modified_by, modified_on)
VALUES (620, 6, '{"hu":"publikus könyvtár (CDN-hez)", "en":"public folder (for CDN)"}', 2, NOW(), 2, NOW());

/* --- */

INSERT INTO code_store_item (id, code_store_type_id, caption, created_by, created_on, modified_by, modified_on)
VALUES (700, 7, '{"hu":"admin vagy létrehozó", "en":"admin or creator user"}', 2, NOW(), 2, NOW());

INSERT INTO code_store_item (id, code_store_type_id, caption, created_by, created_on, modified_by, modified_on)
VALUES (702, 7, '{"hu":"bejelentkezett felhasználó olvasás; admin vagy létrehozója módosítás", "en":"authenticated user read; admin or creater modfiy"}', 2, NOW(), 2, NOW());

INSERT INTO code_store_item (id, code_store_type_id, caption, created_by, created_on, modified_by, modified_on)
VALUES (704, 7, '{"hu":"bejelentkezett felhasználó", "en":"authenticated user"}', 2, NOW(), 2, NOW());

/* --- */

INSERT INTO code_store_item (id, code_store_type_id, caption, created_by, created_on, modified_by, modified_on)
VALUES (800, 8, '{"hu":"otthoni email", "en":"home email"}', 2, NOW(), 2, NOW());

INSERT INTO code_store_item (id, code_store_type_id, caption, created_by, created_on, modified_by, modified_on)
VALUES (802, 8, '{"hu":"munkahelyi email", "en":"work email"}', 2, NOW(), 2, NOW());

INSERT INTO code_store_item (id, code_store_type_id, caption, created_by, created_on, modified_by, modified_on)
VALUES (804, 8, '{"hu":"privát telefonszám", "en":"private phone"}', 2, NOW(), 2, NOW());

INSERT INTO code_store_item (id, code_store_type_id, caption, created_by, created_on, modified_by, modified_on)
VALUES (806, 8, '{"hu":"skype", "en":"skype"}', 2, NOW(), 2, NOW());

INSERT INTO code_store_item (id, code_store_type_id, caption, created_by, created_on, modified_by, modified_on)
VALUES (808, 8, '{"hu":"közösségi média", "en":"social media"}', 2, NOW(), 2, NOW());

/* --- */

INSERT INTO code_store_item (id, code_store_type_id, caption, created_by, created_on, modified_by, modified_on)
VALUES (900, 9, '{"hu":"otthoni cím", "en":"home address"}', 2, NOW(), 2, NOW());

INSERT INTO code_store_item (id, code_store_type_id, caption, created_by, created_on, modified_by, modified_on)
VALUES (902, 9, '{"hu":"munkahelyi cím", "en":"work address"}', 2, NOW(), 2, NOW());

INSERT INTO code_store_item (id, code_store_type_id, caption, created_by, created_on, modified_by, modified_on)
VALUES (904, 9, '{"hu":"cég székhelye", "en":"company seat"}', 2, NOW(), 2, NOW());

INSERT INTO code_store_item (id, code_store_type_id, caption, created_by, created_on, modified_by, modified_on)
VALUES (906, 9, '{"hu":"cég telephely", "en":"company site"}', 2, NOW(), 2, NOW());

/* --- */

INSERT INTO code_store_item (id, code_store_type_id, caption, created_by, created_on, modified_by, modified_on)
VALUES (10000, 10, '{"hu":"ország", "en":"country"}', 2, NOW(), 2, NOW());

INSERT INTO code_store_item (id, code_store_type_id, caption, created_by, created_on, modified_by, modified_on)
VALUES (10002, 10, '{"hu":"város", "en":"city"}', 2, NOW(), 2, NOW());

INSERT INTO code_store_item (id, code_store_type_id, caption, created_by, created_on, modified_by, modified_on)
VALUES (10004, 10, '{"hu":"utca", "en":"street"}', 2, NOW(), 2, NOW());

SELECT SETVAL('code_store_item_id_seq', 50000);