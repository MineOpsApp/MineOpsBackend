ALTER TABLE sites ADD COLUMN insurance_enabled BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE sites ADD COLUMN insurance_provider_name VARCHAR(255);
ALTER TABLE sites ADD COLUMN insurance_premium DECIMAL(10,2);
ALTER TABLE sites ADD COLUMN insurance_deduction_mode VARCHAR(20) DEFAULT 'DEDUCT_FROM_PAY';

ALTER TABLE app_users ADD COLUMN insurance_status VARCHAR(20) DEFAULT 'NOT_INSURED';
ALTER TABLE app_users ADD COLUMN insurance_enrolled_at TIMESTAMP;

CREATE TABLE insurance_enrollment_history (
    id BIGSERIAL PRIMARY KEY,
    worker_email VARCHAR(255) NOT NULL,
    site VARCHAR(255) NOT NULL,
    action VARCHAR(20) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT now()
);
