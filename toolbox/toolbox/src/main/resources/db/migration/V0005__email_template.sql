CREATE TABLE email_template (
	tenant_id INT NOT NULL REFERENCES tenant(id),
	id SERIAL PRIMARY KEY,
	templ_code INT NOT NULL REFERENCES code_store_item(id),
	templ_content JSONB NOT NULL,
	subject JSONB,
	enabled BOOLEAN, -- null-t használunk FALSE helyett a UNIQUE miatt
	created_by INT NOT NULL REFERENCES auth_user,
	created_on TIMESTAMP NOT NULL,
	modified_by INT NOT NULL REFERENCES auth_user,
	modified_on TIMESTAMP NOT NULL,
	UNIQUE(tenant_id, templ_code) DEFERRABLE INITIALLY IMMEDIATE
);

-- email templates - user

INSERT INTO email_template (tenant_id, templ_code, templ_content, enabled, created_by, created_on, modified_by, modified_on, subject)
VALUES (1, 500, '{"hu":"<p> Tisztelt $recipient!</p><p><br></p><p>Felhasználói fiókja sikeresen elkészült.</p><p>A következő jelszóval be tud jelentkezni: $data</p><p><br></p><p><strong>Tisztelettel,</strong></p><p>$tenant</p><p>$appname</p>", "en":"<p>Dear $recipient!</p><p><br></p><p>Your account has been created.</p><p>You can access it with this password: $data</p><p><br></p><p><strong>Best regards,</strong></p><p>$tenant</p><p>$appname</p>"}', TRUE, 2, NOW(), 2, NOW(), '{"hu": "Felhasználó létrehozva", "en:": "User created"}');

INSERT INTO email_template (tenant_id, templ_code, templ_content, enabled, created_by, created_on, modified_by, modified_on, subject)
VALUES (1, 502, '{"hu":"<p>Tisztelt $recipient!</p><p><br></p><p>Az alábbi felhasználó regisztrált a rendszerbe: $data.</p><p>A felhasználó engedélyezéséhez jelentkezzen be a felületre.</p><p><br></p><p><strong>Tisztelettel,</strong></p><p>$tenant</p><p>$appname</p><p><br></p><p>$currentDate</p>", "en": "<p>Dear $recipient!</p><p><br></p><p>A new user has registered: $data.</p><p>If you would like to enable this user, please log in and check the user section.</p><p><br></p><p><strong>Best regards,</strong></p><p>$tenant</p><p>$appname</p><p><br></p><p>$currentDate</p>"}', TRUE, 2, NOW(), 2, NOW(), '{"hu": "Új felhasználó regisztráció", "en:": "New user registration"}');

INSERT INTO email_template (tenant_id, templ_code, templ_content, enabled, created_by, created_on, modified_by, modified_on, subject)
VALUES (1, 504, '{"hu":"<p>Tisztelt $recipient!</p><p><br></p><p>Az alábbi felhasználó sikeresen regisztrált a rendszerbe: $data.</p><p>Ez az üzenet egy automatikus értesítő felhasználó regisztrációjáról (önnek nincs teendője).</p><p><br></p><p><strong>Tisztelettel,</strong></p><p>$tenant</p><p>$appname</p><p><br></p><p>$currentDate</p>", "en": "<p>Dear $recipient!</p><p><br></p><p>A new user has successfully registered: $data.</p><p>This is an automated message about user registration (You do not need to do anything).</p><p><br></p><p><strong>Best regards,</strong></p><p>$tenant</p><p>$appname</p><p><br></p><p>$currentDate</p>"}', TRUE, 2, NOW(), 2, NOW(), '{"hu": "Új felhasználó regisztrált a rendszerbe", "en:": "New user registered"}');

INSERT INTO email_template (tenant_id, templ_code, templ_content, enabled, created_by, created_on, modified_by, modified_on, subject)
VALUES (1, 506, '{"hu": "<p>Tisztelt $recipient!</p><p><br></p><p>E-mail címének megadásával új jelszó létrehozását kérték. Jelszavát a következő linkre kattintva változtathatja meg: $data</p><p>Amennyiben nem Ön kezdeményezte a jelszó megváltoztatását, úgy tekintse ezt az e-mailt tárgytalannak.</p><p><br></p><p><strong>Tisztelettel,</strong></p><p>$tenant</p><p>$appname</p><p><br></p><p>$currentDate</p>", "en": "<p>Dear $recipient!</p><p><br></p><p>A password change request has been created using this email address. You can change your password with the following link: $data</p><p>If you did not initiate this change, please ignore this message.</p><p><br></p><p><strong>Best regards,</strong></p><p>$tenant</p><p>$appname</p><p><br></p><p>$currentDate</p>"}', TRUE, 2, NOW(), 2, NOW(), '{"hu": "Elfelejtett jelszó", "en:": "Forgotten password"}');
