ALTER TABLE quick_contact ADD COLUMN is_sent BOOLEAN;

INSERT INTO code_store_item (id, code_store_type_id, caption, created_by, created_on, modified_by, modified_on)
VALUES (508, 5, '{"hu":"névjegy értesítés", "en":"quick contact notification"}', 2, NOW(), 2, NOW());

INSERT INTO email_template (templ_code, templ_content, enabled, created_by, created_on, modified_by, modified_on, subject)
VALUES (508, '{"hu":"<p> Tisztelt $recipient!</p><p><br></p><p>A következő névjegy(ek) kerültek be a rendszerbe:</p>$data</p><p><br></p><p><strong>Tisztelettel,</strong></p><p>$tenant</p><p>$appname</p>", "en":"<p>Dear $recipient!</p><p><br></p><p>The following contact(s) has been added: $data</p><p><br></p><p><strong>Best regards,</strong></p><p>$tenant</p><p>$appname</p>"}', TRUE, 2, NOW(), 2, NOW(), '{"hu": "Új névjegyek", "en:": "New quick contacts"}');