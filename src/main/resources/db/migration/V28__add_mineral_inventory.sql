-- Approval columns on shift_logs
ALTER TABLE shift_logs ADD COLUMN IF NOT EXISTS approved_by  VARCHAR(255);
ALTER TABLE shift_logs ADD COLUMN IF NOT EXISTS approved_at  TIMESTAMP;
ALTER TABLE shift_logs ADD COLUMN IF NOT EXISTS rejected_by  VARCHAR(255);
ALTER TABLE shift_logs ADD COLUMN IF NOT EXISTS rejected_at  TIMESTAMP;

-- Running inventory totals — one row per (site, mineral_type, unit) combination
CREATE TABLE mineral_inventory (
    id               BIGSERIAL PRIMARY KEY,
    site             VARCHAR(255)   NOT NULL,
    mineral_type     VARCHAR(255)   NOT NULL,
    unit             VARCHAR(50)    NOT NULL,
    total_volume     DECIMAL(15,4)  NOT NULL DEFAULT 0,
    last_updated_at  TIMESTAMP,
    last_shift_log_id BIGINT,
    last_worker_name VARCHAR(255),
    last_zone        VARCHAR(255),
    UNIQUE (site, mineral_type, unit)
);

-- Immutable audit trail — one row per approved shift log contribution
CREATE TABLE inventory_transactions (
    id           BIGSERIAL PRIMARY KEY,
    site         VARCHAR(255)  NOT NULL,
    mineral_type VARCHAR(255)  NOT NULL,
    unit         VARCHAR(50)   NOT NULL,
    volume_added DECIMAL(15,4) NOT NULL,
    shift_log_id BIGINT        NOT NULL REFERENCES shift_logs(id),
    worker_name  VARCHAR(255)  NOT NULL,
    worker_email VARCHAR(255)  NOT NULL,
    zone         VARCHAR(255),
    approved_by  VARCHAR(255),
    created_at   TIMESTAMP     NOT NULL
);

CREATE INDEX idx_mineral_inventory_site          ON mineral_inventory(site);
CREATE INDEX idx_inventory_tx_site               ON inventory_transactions(site);
CREATE INDEX idx_inventory_tx_worker             ON inventory_transactions(worker_email);
CREATE INDEX idx_inventory_tx_shift_log          ON inventory_transactions(shift_log_id);
