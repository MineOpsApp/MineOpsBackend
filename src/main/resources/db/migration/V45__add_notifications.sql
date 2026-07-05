CREATE TABLE notification (
    id BIGSERIAL PRIMARY KEY,
    recipient_email VARCHAR(255) NOT NULL,
    type VARCHAR(30) NOT NULL,
    title VARCHAR(255) NOT NULL,
    body VARCHAR(500) NOT NULL,
    related_entity_type VARCHAR(50),
    related_entity_id BIGINT,
    read_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT now()
);
CREATE INDEX idx_notification_recipient ON notification(recipient_email, created_at DESC);
CREATE INDEX idx_notification_unread ON notification(recipient_email, read_at);
