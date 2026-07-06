ALTER TABLE app_users ADD COLUMN government_agency VARCHAR(50);

CREATE TABLE mining_permit_status (
    id BIGSERIAL PRIMARY KEY,
    site VARCHAR(255) NOT NULL UNIQUE,
    application_submitted BOOLEAN NOT NULL DEFAULT FALSE,
    community_notification_done BOOLEAN NOT NULL DEFAULT FALSE,
    ministerial_review_status VARCHAR(30),
    epa_permit_obtained BOOLEAN NOT NULL DEFAULT FALSE,
    updated_by_email VARCHAR(255),
    updated_at TIMESTAMP
);

CREATE TABLE bulk_purchase_request (
    id BIGSERIAL PRIMARY KEY,
    site VARCHAR(255) NOT NULL,
    mineral_type VARCHAR(100) NOT NULL,
    quantity_available DECIMAL(14,3) NOT NULL,
    unit VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE',
    flagged_by_email VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE illegal_mine_report (
    id BIGSERIAL PRIMARY KEY,
    reporter_email VARCHAR(255) NOT NULL,
    reporter_role VARCHAR(20) NOT NULL,
    location_description VARCHAR(500) NOT NULL,
    details VARCHAR(1500),
    status VARCHAR(20) NOT NULL DEFAULT 'SUBMITTED',
    reviewed_by_email VARCHAR(255),
    review_notes VARCHAR(1000),
    created_at TIMESTAMP NOT NULL DEFAULT now()
);
