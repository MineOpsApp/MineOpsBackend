ALTER TABLE drill_operations ADD COLUMN IF NOT EXISTS blast_decision VARCHAR(20);
ALTER TABLE drill_operations ADD COLUMN IF NOT EXISTS blast_decision_note VARCHAR(500);

-- Existing rows that already have an approver were approved under the old binary flow.
UPDATE drill_operations SET blast_decision = 'APPROVED' WHERE blast_decision IS NULL AND blast_approved_by IS NOT NULL;
