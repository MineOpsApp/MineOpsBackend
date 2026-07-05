CREATE TABLE supervisor_site_access (
    id BIGSERIAL PRIMARY KEY,
    supervisor_email VARCHAR(255) NOT NULL,
    site VARCHAR(255) NOT NULL,
    granted_by_email VARCHAR(255) NOT NULL,
    granted_at TIMESTAMP NOT NULL DEFAULT now(),
    UNIQUE(supervisor_email, site)
);
CREATE INDEX idx_site_access_supervisor ON supervisor_site_access(supervisor_email);

ALTER TABLE app_users ADD COLUMN home_site VARCHAR(255);
UPDATE app_users SET home_site = assigned_site WHERE role = 'supervisor';
