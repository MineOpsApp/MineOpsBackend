CREATE TABLE certifications (
    id                  BIGSERIAL    PRIMARY KEY,
    worker_id           BIGINT       NOT NULL REFERENCES app_users(id) ON DELETE CASCADE,
    worker_name         VARCHAR(255) NOT NULL,
    worker_email        VARCHAR(255) NOT NULL,
    site                VARCHAR(255) NOT NULL,
    certification_name  VARCHAR(255) NOT NULL,
    issuing_authority   VARCHAR(255) NOT NULL,
    issue_date          DATE         NOT NULL,
    expiry_date         DATE         NOT NULL,
    notes               TEXT,
    created_at          TIMESTAMP    NOT NULL,
    updated_at          TIMESTAMP    NOT NULL,
    created_by          VARCHAR(255) NOT NULL
);

CREATE TABLE certification_history (
    id                  BIGSERIAL    PRIMARY KEY,
    certification_id    BIGINT       NOT NULL REFERENCES certifications(id) ON DELETE CASCADE,
    previous_expiry     DATE,
    new_expiry          DATE         NOT NULL,
    previous_authority  VARCHAR(255),
    new_authority       VARCHAR(255),
    renewed_by          VARCHAR(255) NOT NULL,
    renewed_at          TIMESTAMP    NOT NULL,
    notes               TEXT
);

CREATE INDEX idx_cert_site      ON certifications(site);
CREATE INDEX idx_cert_worker_id ON certifications(worker_id);
CREATE INDEX idx_cert_expiry    ON certifications(expiry_date);
CREATE INDEX idx_cert_history   ON certification_history(certification_id);
