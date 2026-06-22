ALTER TABLE hazard_reports ADD COLUMN severity VARCHAR(50);
UPDATE hazard_reports SET severity = 'Medium' WHERE severity IS NULL;
