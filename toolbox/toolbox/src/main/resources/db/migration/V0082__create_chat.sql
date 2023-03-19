INSERT INTO code_store_type (id, caption, created_by, created_on, modified_by, modified_on, expandable)
VALUES (21, '{"hu":"chat cél típusa", "en":"chat target type"}', 2, NOW(), 2, NOW(), TRUE);

INSERT INTO code_store_item (id, code_store_type_id, caption, created_by, created_on, modified_by, modified_on)
VALUES (2108, 21, '{"hu":"jogosultság", "en":"role"}', 2, NOW(), 2, NOW());

INSERT INTO code_store_item (id, code_store_type_id, caption, created_by, created_on, modified_by, modified_on)
VALUES (2110, 21, '{"hu":"felhasználó", "en":"user"}', 2, NOW(), 2, NOW());

CREATE TABLE chat_entry (
  tenant_id INT NOT NULL REFERENCES tenant(id),
  id SERIAL PRIMARY KEY,
  target_type INT NOT NULL REFERENCES code_store_item(id), 
  target_value VARCHAR(255) NOT NULL,  
  message_text VARCHAR(3000) NOT NULL,
  file_ids JSONB,
  seen BOOLEAN,
  created_by INT NOT NULL REFERENCES auth_user,
  created_on TIMESTAMP NOT NULL,
  modified_by INT NOT NULL REFERENCES auth_user,
  modified_on TIMESTAMP NOT NULL
);