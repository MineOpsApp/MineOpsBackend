ALTER TABLE hazard_reports ADD COLUMN client_request_id VARCHAR(64);
ALTER TABLE sos_alerts ADD COLUMN client_request_id VARCHAR(64);
ALTER TABLE attendance_records ADD COLUMN client_request_id VARCHAR(64);
ALTER TABLE shift_logs ADD COLUMN client_request_id VARCHAR(64);

CREATE UNIQUE INDEX idx_hazard_client_request ON hazard_reports(client_request_id) WHERE client_request_id IS NOT NULL;
CREATE UNIQUE INDEX idx_sos_client_request ON sos_alerts(client_request_id) WHERE client_request_id IS NOT NULL;
CREATE UNIQUE INDEX idx_attendance_client_request ON attendance_records(client_request_id) WHERE client_request_id IS NOT NULL;
CREATE UNIQUE INDEX idx_shiftlog_client_request ON shift_logs(client_request_id) WHERE client_request_id IS NOT NULL;
