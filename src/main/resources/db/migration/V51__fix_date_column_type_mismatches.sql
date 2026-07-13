ALTER TABLE pay_cycle ALTER COLUMN period_start TYPE VARCHAR(255) USING period_start::text;
ALTER TABLE pay_cycle ALTER COLUMN period_end TYPE VARCHAR(255) USING period_end::text;
ALTER TABLE shift_logs ALTER COLUMN shift_date TYPE VARCHAR(30) USING shift_date::text;
