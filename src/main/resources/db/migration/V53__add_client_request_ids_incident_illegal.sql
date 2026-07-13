ALTER TABLE incident_reports ADD COLUMN client_request_id VARCHAR(64);
ALTER TABLE illegal_mine_report ADD COLUMN client_request_id VARCHAR(64);

CREATE UNIQUE INDEX idx_incident_client_request ON incident_reports(client_request_id) WHERE client_request_id IS NOT NULL;
CREATE UNIQUE INDEX idx_illegal_report_client_request ON illegal_mine_report(client_request_id) WHERE client_request_id IS NOT NULL;
