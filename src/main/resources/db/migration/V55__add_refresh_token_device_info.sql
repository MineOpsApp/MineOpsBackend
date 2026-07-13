ALTER TABLE refresh_tokens ADD COLUMN device_name VARCHAR(120);
ALTER TABLE refresh_tokens ADD COLUMN platform VARCHAR(40);
ALTER TABLE refresh_tokens ADD COLUMN last_used_at TIMESTAMP;
