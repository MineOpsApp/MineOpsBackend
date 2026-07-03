CREATE TABLE shift_announcements (
    id              BIGSERIAL PRIMARY KEY,
    site            VARCHAR(255) NOT NULL,
    content         VARCHAR(200) NOT NULL,
    created_by_name  VARCHAR(255) NOT NULL,
    created_by_email VARCHAR(255) NOT NULL,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_announcements_site_created ON shift_announcements(site, created_at DESC);
