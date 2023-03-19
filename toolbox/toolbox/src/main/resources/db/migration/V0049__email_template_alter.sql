UPDATE code_store_item 
SET caption = '{"hu":"quick contact megkeresésekről értesítés", "en":"notification about incoming quick contacts"}' 
WHERE id = 508;

UPDATE email_template SET 
templ_content = '{"hu":"<p>Tisztelt $recipient!</p><p><br></p><p>A következő Quick contact megkeresések (publikus formon rögzített) kerültek be a rendszerbe:</p>$data</p><p><br></p><p><strong>Tisztelettel,</strong></p><p>$tenantName</p><p>$appName</p>", "en":"<p>Dear $recipient!</p><p><br></p><p>The following incoming (from public form) Quick contact elements have been added: $data</p><p><br></p><p><strong>Best regards,</strong></p><p>$tenantName</p><p>$appName</p>"}', 
subject = '{"hu": "Új publikus form Quick contact megkeresések", "en:": "New public form Quick contact records"}' 
WHERE templ_code = 508;