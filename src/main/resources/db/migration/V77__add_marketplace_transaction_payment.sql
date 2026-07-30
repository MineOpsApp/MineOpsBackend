ALTER TABLE marketplace_transaction ADD COLUMN payment_status VARCHAR(20) NOT NULL DEFAULT 'UNPAID';
ALTER TABLE marketplace_transaction ADD COLUMN paystack_reference VARCHAR(100);
ALTER TABLE marketplace_transaction ADD COLUMN paid_amount NUMERIC(14,2);
ALTER TABLE marketplace_transaction ADD COLUMN paid_at TIMESTAMP;
ALTER TABLE marketplace_transaction ADD COLUMN payment_channel VARCHAR(30);

-- Lookup index for the webhook/verify path (find-by-reference), not unique — a buyer can retry
-- a FAILED attempt and get a fresh reference, but nothing here strictly forbids reuse.
CREATE INDEX idx_marketplace_transaction_paystack_reference ON marketplace_transaction(paystack_reference);
