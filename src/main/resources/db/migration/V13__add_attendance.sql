CREATE TABLE attendance_records (
    id BIGSERIAL PRIMARY KEY,
    worker_email VARCHAR(255),
    worker_name VARCHAR(255),
    worker_role VARCHAR(50),
    site VARCHAR(255),
    zone VARCHAR(100),
    status VARCHAR(20) NOT NULL DEFAULT 'ON_SITE',
    clock_in_at TIMESTAMP NOT NULL DEFAULT now(),
    clock_out_at TIMESTAMP,
    notes TEXT
);

CREATE INDEX idx_attendance_site_status ON attendance_records(site, status);
CREATE INDEX idx_attendance_worker_email ON attendance_records(worker_email);

