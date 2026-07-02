CREATE TABLE emergency_contacts (
    id BIGSERIAL PRIMARY KEY,
    worker_id BIGINT NOT NULL REFERENCES app_users(id) ON DELETE CASCADE,
    contact_type VARCHAR(10) NOT NULL CHECK (contact_type IN ('PRIMARY', 'BACKUP')),
    name VARCHAR(255) NOT NULL,
    relationship VARCHAR(100) NOT NULL,
    phone VARCHAR(50) NOT NULL,
    created_at TIMESTAMP,
    UNIQUE (worker_id, contact_type)
);

CREATE INDEX idx_emergency_contacts_worker_id ON emergency_contacts(worker_id);
