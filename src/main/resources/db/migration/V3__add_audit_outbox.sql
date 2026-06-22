CREATE TABLE audit_outbox (
    id BIGSERIAL PRIMARY KEY,
    action VARCHAR(255) NOT NULL,
    actor_role VARCHAR(255),
    actor_name VARCHAR(255),
    actor_email VARCHAR(255),
    target_type VARCHAR(255),
    target_id BIGINT,
    details TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    attempts INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    last_attempt_at TIMESTAMP
);
