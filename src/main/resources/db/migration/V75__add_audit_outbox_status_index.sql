-- AuditOutboxRelay.relayPending() runs findTop50ByStatusOrderByCreatedAtAsc('PENDING') every 10
-- seconds via a @Scheduled job, with no index on status — every poll is a full sequential scan.
-- Harmless while the table is tiny, but it grows without bound in normal operation (rows only
-- leave PENDING once relayed) and gets worse under any audit-service outage, when entries pile up
-- as PENDING/FAILED instead of draining. Composite index matches the query's filter + sort.
CREATE INDEX IF NOT EXISTS idx_audit_outbox_status_created ON audit_outbox(status, created_at);
