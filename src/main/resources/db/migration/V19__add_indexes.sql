CREATE INDEX IF NOT EXISTS idx_hazard_reports_site ON hazard_reports(site);
CREATE INDEX IF NOT EXISTS idx_hazard_reports_status ON hazard_reports(status);
CREATE INDEX IF NOT EXISTS idx_hazard_reports_email ON hazard_reports(reported_by_email);

CREATE INDEX IF NOT EXISTS idx_shift_logs_site ON shift_logs(site);
CREATE INDEX IF NOT EXISTS idx_shift_logs_email ON shift_logs(worker_email);

CREATE INDEX IF NOT EXISTS idx_attendance_site_status ON attendance_records(site, status);
CREATE INDEX IF NOT EXISTS idx_attendance_email ON attendance_records(worker_email);

CREATE INDEX IF NOT EXISTS idx_incident_reports_site ON incident_reports(site);
CREATE INDEX IF NOT EXISTS idx_incident_reports_status ON incident_reports(status);
CREATE INDEX IF NOT EXISTS idx_incident_reports_email ON incident_reports(reported_by_email);

CREATE INDEX IF NOT EXISTS idx_blast_schedules_site_status ON blast_schedules(site, status);

CREATE INDEX IF NOT EXISTS idx_notices_created_at ON notices(created_at);

CREATE INDEX IF NOT EXISTS idx_app_users_site ON app_users(assigned_site);
CREATE INDEX IF NOT EXISTS idx_app_users_role ON app_users(role);

