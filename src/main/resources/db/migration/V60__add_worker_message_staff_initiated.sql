ALTER TABLE worker_messages ADD COLUMN recipient_email VARCHAR(255);
ALTER TABLE worker_messages ADD COLUMN recipient_name VARCHAR(255);
ALTER TABLE worker_messages ADD COLUMN initiated_by VARCHAR(20) NOT NULL DEFAULT 'WORKER';
