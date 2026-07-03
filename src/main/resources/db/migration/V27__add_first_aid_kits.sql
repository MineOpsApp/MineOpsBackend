CREATE TABLE first_aid_kits (
    id BIGSERIAL PRIMARY KEY,
    site VARCHAR(255) NOT NULL,
    zone VARCHAR(255) NOT NULL,
    location VARCHAR(255) NOT NULL,
    has_bandages BOOLEAN NOT NULL DEFAULT FALSE,
    has_gloves BOOLEAN NOT NULL DEFAULT FALSE,
    has_antiseptic BOOLEAN NOT NULL DEFAULT FALSE,
    has_oxygen BOOLEAN NOT NULL DEFAULT FALSE,
    has_stretcher BOOLEAN NOT NULL DEFAULT FALSE,
    notes VARCHAR(500),
    last_checked_by VARCHAR(255),
    last_checked_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL,
    UNIQUE (site, zone)
);

CREATE INDEX idx_first_aid_kits_site ON first_aid_kits(site);
