-- Visitor visits table
CREATE TABLE visitor_visits (
    id                        BIGSERIAL PRIMARY KEY,
    guest_user_id             BIGINT NOT NULL,
    host_name                 VARCHAR(255),
    purpose_of_visit          VARCHAR(500),
    assigned_site             VARCHAR(255),
    visitor_pass_number       VARCHAR(50) UNIQUE,
    visit_start               TIMESTAMP,
    visit_end                 TIMESTAMP,
    approved_zones            TEXT,
    induction_completed       BOOLEAN NOT NULL DEFAULT FALSE,
    induction_completed_at    TIMESTAMP,
    induction_sign_off        VARCHAR(255),
    emergency_contact_name    VARCHAR(255),
    emergency_contact_phone   VARCHAR(255),
    ppe_issued                BOOLEAN NOT NULL DEFAULT FALSE,
    ppe_items                 VARCHAR(500),
    check_in_at               TIMESTAMP,
    check_out_at              TIMESTAMP,
    zones_visited             TEXT,
    status                    VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    visiting_organisation     VARCHAR(255),
    relationship_to_host      VARCHAR(255),
    visit_reason              VARCHAR(100),
    vehicle_registration_number VARCHAR(50),
    group_size                INTEGER,
    medical_conditions_note   VARCHAR(500),
    created_at                TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Common identity field for all users
ALTER TABLE app_users ADD COLUMN nationality VARCHAR(100);

-- Buyer-specific profile fields
ALTER TABLE app_users ADD COLUMN company_registration_number VARCHAR(100);
ALTER TABLE app_users ADD COLUMN country_of_incorporation VARCHAR(100);
ALTER TABLE app_users ADD COLUMN business_type VARCHAR(50);
ALTER TABLE app_users ADD COLUMN position_title VARCHAR(100);
ALTER TABLE app_users ADD COLUMN government_id_document TEXT;
ALTER TABLE app_users ADD COLUMN export_licence_number VARCHAR(100);
ALTER TABLE app_users ADD COLUMN operating_jurisdiction VARCHAR(255);
ALTER TABLE app_users ADD COLUMN minerals_of_interest VARCHAR(500);
ALTER TABLE app_users ADD COLUMN typical_order_volume VARCHAR(255);
ALTER TABLE app_users ADD COLUMN preferred_transaction_method VARCHAR(50);
ALTER TABLE app_users ADD COLUMN nda_signed BOOLEAN NOT NULL DEFAULT FALSE;
