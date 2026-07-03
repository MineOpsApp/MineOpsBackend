CREATE TABLE guest_access_code (
    id BIGSERIAL PRIMARY KEY,
    site VARCHAR(255) NOT NULL,
    guest_sub_role VARCHAR(20) NOT NULL,
    code VARCHAR(10) NOT NULL UNIQUE,
    session_hours INT NOT NULL DEFAULT 24,
    max_redemptions INT NOT NULL DEFAULT 1,
    redemption_count INT NOT NULL DEFAULT 0,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_by VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    expires_at TIMESTAMP NOT NULL
);
CREATE INDEX idx_guest_code_site ON guest_access_code(site);

ALTER TABLE app_users ADD COLUMN redeemed_code_id BIGINT REFERENCES guest_access_code(id);
ALTER TABLE app_users ADD COLUMN induction_completed_at TIMESTAMP;
ALTER TABLE app_users ADD COLUMN phone VARCHAR(20);
