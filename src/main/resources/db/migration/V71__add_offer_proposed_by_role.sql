ALTER TABLE marketplace_offer ADD COLUMN IF NOT EXISTS proposed_by_role VARCHAR(20);
-- Existing rows with a parent_offer_id are counter-offers that were created by a supervisor
-- under the old code path; everything else was buyer-submitted.
UPDATE marketplace_offer SET proposed_by_role = 'SUPERVISOR' WHERE proposed_by_role IS NULL AND parent_offer_id IS NOT NULL;
UPDATE marketplace_offer SET proposed_by_role = 'BUYER' WHERE proposed_by_role IS NULL;
ALTER TABLE marketplace_offer ADD COLUMN IF NOT EXISTS rejection_reason VARCHAR(500);
