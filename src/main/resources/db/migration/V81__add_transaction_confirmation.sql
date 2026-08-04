ALTER TABLE marketplace_transaction ADD COLUMN IF NOT EXISTS buyer_confirmed_at TIMESTAMP;
ALTER TABLE marketplace_transaction ADD COLUMN IF NOT EXISTS supervisor_confirmed_at TIMESTAMP;
ALTER TABLE marketplace_transaction ADD COLUMN IF NOT EXISTS closed BOOLEAN DEFAULT FALSE;
