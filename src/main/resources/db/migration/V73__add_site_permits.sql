CREATE TABLE site_permits (
    id BIGSERIAL PRIMARY KEY,
    site VARCHAR(255) NOT NULL,
    permit_name VARCHAR(255) NOT NULL,
    permit_number VARCHAR(255),
    issuing_authority VARCHAR(255) NOT NULL,
    issue_date DATE NOT NULL,
    expiry_date DATE NOT NULL,
    notes VARCHAR(255),
    document_data TEXT,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    created_by VARCHAR(255) NOT NULL
);

CREATE INDEX idx_site_permits_site ON site_permits (site);
