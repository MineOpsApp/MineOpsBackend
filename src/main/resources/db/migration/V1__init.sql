CREATE TABLE app_users (
    id BIGSERIAL PRIMARY KEY,
    full_name VARCHAR(255),
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(255) NOT NULL,
    created_at TIMESTAMP
);

CREATE TABLE sites (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255),
    status VARCHAR(255)
);

CREATE TABLE hazard_reports (
    id BIGSERIAL PRIMARY KEY,
    reported_by_role VARCHAR(255),
    reported_by_name VARCHAR(255),
    reported_by_email VARCHAR(255),
    hazard_type VARCHAR(255),
    site VARCHAR(255),
    location VARCHAR(255),
    description TEXT,
    status VARCHAR(255),
    reviewed_by_role VARCHAR(255),
    reviewed_by_name VARCHAR(255),
    reviewed_by_email VARCHAR(255),
    closed_by_role VARCHAR(255),
    closed_by_name VARCHAR(255),
    closed_by_email VARCHAR(255),
    action_taken TEXT,
    created_at TIMESTAMP,
    reviewed_at TIMESTAMP,
    closed_at TIMESTAMP
);

CREATE TABLE danger_zones (
    id BIGSERIAL PRIMARY KEY,
    site VARCHAR(255),
    zone_name VARCHAR(255),
    risk_level VARCHAR(255),
    status VARCHAR(255),
    created_at TIMESTAMP
);

CREATE TABLE sos_alerts (
    id BIGSERIAL PRIMARY KEY,
    role VARCHAR(255),
    site VARCHAR(255),
    message TEXT,
    status VARCHAR(255),
    created_at TIMESTAMP
);

CREATE TABLE notices (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(255),
    message TEXT,
    posted_by_role VARCHAR(255),
    created_at TIMESTAMP
);

CREATE TABLE notice_seen (
    id BIGSERIAL PRIMARY KEY,
    notice_id BIGINT,
    full_name VARCHAR(255),
    email VARCHAR(255),
    role VARCHAR(255),
    seen_at TIMESTAMP
);

CREATE TABLE worker_equipment (
    id BIGSERIAL PRIMARY KEY,
    worker_email VARCHAR(255),
    name VARCHAR(255),
    code VARCHAR(255),
    status VARCHAR(255),
    instructions TEXT
);

CREATE TABLE equipment_faults (
    id BIGSERIAL PRIMARY KEY,
    worker_email VARCHAR(255),
    equipment_code VARCHAR(255),
    description TEXT,
    status VARCHAR(255),
    created_at TIMESTAMP
);

CREATE TABLE maintenance_requests (
    id BIGSERIAL PRIMARY KEY,
    worker_email VARCHAR(255),
    equipment_code VARCHAR(255),
    request_details TEXT,
    status VARCHAR(255),
    created_at TIMESTAMP
);

CREATE TABLE supervisor_messages (
    id BIGSERIAL PRIMARY KEY,
    sender_role VARCHAR(255),
    audience VARCHAR(255),
    message TEXT,
    created_at TIMESTAMP
);

CREATE TABLE visitor_inductions (
    id BIGSERIAL PRIMARY KEY,
    visitor_type VARCHAR(255),
    site VARCHAR(255),
    status VARCHAR(255),
    completed_at TIMESTAMP
);