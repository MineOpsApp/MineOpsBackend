ALTER TABLE pay_cycle RENAME COLUMN pay_date TO period_start;
ALTER TABLE pay_cycle ADD COLUMN IF NOT EXISTS period_end DATE;
UPDATE pay_cycle SET period_end = period_start + 6 WHERE period_end IS NULL;
ALTER TABLE pay_cycle ALTER COLUMN period_end SET NOT NULL;
