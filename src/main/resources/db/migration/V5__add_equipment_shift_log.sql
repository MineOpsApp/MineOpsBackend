CREATE TABLE equipment_shift_logs (
    id BIGSERIAL PRIMARY KEY,
    equipment_code VARCHAR(255),
    equipment_name VARCHAR(255),
    worker_email VARCHAR(255),
    worker_name VARCHAR(255),
    status VARCHAR(50) NOT NULL,
    check_type VARCHAR(50) NOT NULL,
    notes TEXT,
    logged_at TIMESTAMP NOT NULL DEFAULT now()
);
