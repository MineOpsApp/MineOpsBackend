CREATE TABLE blast_schedules (
    id BIGSERIAL PRIMARY KEY,
    site VARCHAR(255),
    zone VARCHAR(100),
    scheduled_by VARCHAR(255),
    scheduled_by_name VARCHAR(255),
    notes TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'SCHEDULED',
    blast_time TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now()
);