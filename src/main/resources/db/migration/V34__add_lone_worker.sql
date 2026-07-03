CREATE TABLE lone_worker_sessions (
    id BIGSERIAL PRIMARY KEY,
    worker_id BIGINT NOT NULL,
    worker_email VARCHAR(255) NOT NULL,
    worker_name VARCHAR(255) NOT NULL,
    site VARCHAR(255) NOT NULL,
    interval_minutes INT NOT NULL DEFAULT 60,
    started_at TIMESTAMP NOT NULL DEFAULT NOW(),
    last_checked_in_at TIMESTAMP NOT NULL DEFAULT NOW(),
    deadline TIMESTAMP NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    alerted BOOLEAN NOT NULL DEFAULT FALSE
);
CREATE INDEX idx_lone_worker_active ON lone_worker_sessions(active, site);
CREATE INDEX idx_lone_worker_worker ON lone_worker_sessions(worker_id);
