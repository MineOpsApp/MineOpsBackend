-- Government account-level identity fields
ALTER TABLE app_users ADD COLUMN regulatory_body_name VARCHAR(255);
ALTER TABLE app_users ADD COLUMN official_id_badge_number VARCHAR(100);
ALTER TABLE app_users ADD COLUMN issuing_authority VARCHAR(255);
ALTER TABLE app_users ADD COLUMN official_id_document TEXT;
ALTER TABLE app_users ADD COLUMN jurisdiction_of_authority VARCHAR(255);

-- Inspection records (per inspection, not per account)
CREATE TABLE inspection_records (
    id                          BIGSERIAL PRIMARY KEY,
    inspector_user_id           BIGINT NOT NULL,
    site                        VARCHAR(255),
    inspection_type             VARCHAR(50),
    inspection_reference_number VARCHAR(255),
    scope                       VARCHAR(50),
    legal_authority_reference   VARCHAR(255),
    expected_duration           VARCHAR(100),
    inspection_start_at         TIMESTAMP,
    inspection_end_at           TIMESTAMP,
    zones_inspected             TEXT,
    findings_summary            TEXT,
    compliance_status           VARCHAR(50),
    follow_up_required          BOOLEAN NOT NULL DEFAULT FALSE,
    report_submitted            BOOLEAN NOT NULL DEFAULT FALSE,
    next_inspection_date        DATE,
    created_at                  TIMESTAMP NOT NULL DEFAULT NOW()
);
