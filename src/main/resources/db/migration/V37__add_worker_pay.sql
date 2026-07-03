ALTER TABLE app_users ADD COLUMN IF NOT EXISTS momo_number VARCHAR(20);
ALTER TABLE app_users ADD COLUMN IF NOT EXISTS momo_network VARCHAR(20);

ALTER TABLE shift_logs ADD COLUMN IF NOT EXISTS pay_cycle_id BIGINT;

CREATE TABLE IF NOT EXISTS pay_split_config (
    id BIGSERIAL PRIMARY KEY,
    site VARCHAR(255) NOT NULL UNIQUE,
    formula_type VARCHAR(30) NOT NULL DEFAULT 'EQUAL_PER_HEAD',
    updated_by VARCHAR(255),
    updated_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS pay_cycle (
    id BIGSERIAL PRIMARY KEY,
    site VARCHAR(255) NOT NULL,
    pay_date DATE NOT NULL,
    mineral_type VARCHAR(100) NOT NULL,
    unit VARCHAR(20) NOT NULL,
    total_volume DECIMAL(14,3) NOT NULL,
    price_per_unit DECIMAL(14,2) NOT NULL,
    gross_total DECIMAL(14,2) NOT NULL,
    formula_type VARCHAR(30) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'DRAFT',
    created_by VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    manager_approved_by VARCHAR(255),
    manager_approved_at TIMESTAMP,
    supervisor_approved_by VARCHAR(255),
    supervisor_approved_at TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_pay_cycle_site ON pay_cycle(site);

CREATE TABLE IF NOT EXISTS worker_pay_record (
    id BIGSERIAL PRIMARY KEY,
    pay_cycle_id BIGINT NOT NULL REFERENCES pay_cycle(id),
    worker_email VARCHAR(255) NOT NULL,
    worker_name VARCHAR(255) NOT NULL,
    hours_worked DECIMAL(6,2),
    gross_share DECIMAL(14,2) NOT NULL,
    insurance_deduction DECIMAL(14,2) NOT NULL DEFAULT 0,
    net_pay DECIMAL(14,2) NOT NULL,
    momo_number VARCHAR(20),
    momo_network VARCHAR(20),
    disbursement_status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    momo_transaction_ref VARCHAR(100),
    failure_reason VARCHAR(255),
    disbursed_at TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_worker_pay_cycle ON worker_pay_record(pay_cycle_id);
CREATE INDEX IF NOT EXISTS idx_worker_pay_email ON worker_pay_record(worker_email);
