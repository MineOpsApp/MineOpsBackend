CREATE TABLE subscription_tier (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    monthly_price_ghs DECIMAL(10,2) NOT NULL,
    description VARCHAR(500),
    active BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE site_subscription (
    id BIGSERIAL PRIMARY KEY,
    site VARCHAR(255) NOT NULL UNIQUE,
    tier_id BIGINT REFERENCES subscription_tier(id),
    status VARCHAR(20) NOT NULL DEFAULT 'TRIAL',
    trial_ends_at TIMESTAMP,
    current_period_ends_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE subscription_payment (
    id BIGSERIAL PRIMARY KEY,
    site VARCHAR(255) NOT NULL,
    amount_ghs DECIMAL(10,2) NOT NULL,
    method VARCHAR(30),
    reference VARCHAR(255),
    recorded_by_email VARCHAR(255) NOT NULL,
    period_covered_start DATE,
    period_covered_end DATE,
    created_at TIMESTAMP NOT NULL DEFAULT now()
);
