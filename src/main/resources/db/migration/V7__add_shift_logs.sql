CREATE TABLE shift_logs (
    id BIGSERIAL PRIMARY KEY,
    worker_email VARCHAR(255),
    worker_name VARCHAR(255),
    site VARCHAR(255),
    zone VARCHAR(255),
    shift_type VARCHAR(50),
    mineral_type VARCHAR(100),
    volume_extracted DECIMAL(10,2),
    unit VARCHAR(20),
    equipment_code VARCHAR(100),
    equipment_name VARCHAR(255),
    notes TEXT,
    status VARCHAR(50) NOT NULL DEFAULT 'SUBMITTED',
    shift_date TIMESTAMP,
    submitted_at TIMESTAMP NOT NULL DEFAULT now()
);
