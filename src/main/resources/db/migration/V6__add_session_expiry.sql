ALTER TABLE app_users ADD COLUMN session_expires_at TIMESTAMP;

UPDATE app_users SET session_expires_at = created_at + INTERVAL '24 hours' WHERE role = 'guest';
