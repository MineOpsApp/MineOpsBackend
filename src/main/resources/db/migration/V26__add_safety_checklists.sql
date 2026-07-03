CREATE TABLE safety_checklists (
    id BIGSERIAL PRIMARY KEY,
    worker_id BIGINT NOT NULL REFERENCES app_users(id) ON DELETE CASCADE,
    worker_name VARCHAR(255) NOT NULL,
    worker_email VARCHAR(255) NOT NULL,
    site VARCHAR(255) NOT NULL,
    shift_date DATE NOT NULL,
    ppe_helmet BOOLEAN NOT NULL DEFAULT FALSE,
    ppe_boots BOOLEAN NOT NULL DEFAULT FALSE,
    ppe_gloves BOOLEAN NOT NULL DEFAULT FALSE,
    ppe_vest BOOLEAN NOT NULL DEFAULT FALSE,
    equipment_checked BOOLEAN NOT NULL DEFAULT FALSE,
    communication_device BOOLEAN NOT NULL DEFAULT FALSE,
    emergency_exits_clear BOOLEAN NOT NULL DEFAULT FALSE,
    hazardous_materials_secured BOOLEAN NOT NULL DEFAULT FALSE,
    submitted_at TIMESTAMP NOT NULL,
    UNIQUE (worker_id, shift_date)
);

CREATE INDEX idx_safety_checklists_site_date ON safety_checklists(site, shift_date);
CREATE INDEX idx_safety_checklists_worker_id ON safety_checklists(worker_id);
