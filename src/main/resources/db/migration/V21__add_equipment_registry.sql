CREATE TABLE equipment (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(50) NOT NULL,
    name VARCHAR(255) NOT NULL,
    type VARCHAR(50),
    site VARCHAR(255),
    status VARCHAR(30) NOT NULL DEFAULT 'Operational',
    notes TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX idx_equipment_code_site ON equipment(code, site);
CREATE INDEX idx_equipment_site ON equipment(site);

