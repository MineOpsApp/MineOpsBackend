ALTER TABLE drill_operations
    ADD COLUMN blast_approved_by VARCHAR(255),
    ADD COLUMN blast_approved_by_name VARCHAR(255),
    ADD COLUMN blast_approved_at TIMESTAMP;
