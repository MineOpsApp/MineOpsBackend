ALTER TABLE app_users ADD COLUMN assigned_site VARCHAR(255);

UPDATE app_users SET assigned_site = 'Obuasi Mine' WHERE assigned_site IS NULL;