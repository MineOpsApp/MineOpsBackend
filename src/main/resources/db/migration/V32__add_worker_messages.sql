CREATE TABLE worker_messages (
    id          BIGSERIAL PRIMARY KEY,
    sender_email VARCHAR(255) NOT NULL,
    sender_name  VARCHAR(255) NOT NULL,
    site         VARCHAR(255) NOT NULL,
    content      TEXT NOT NULL,
    reply        TEXT,
    replied_at   TIMESTAMP,
    read_at      TIMESTAMP,
    created_at   TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_worker_messages_site   ON worker_messages(site);
CREATE INDEX idx_worker_messages_sender ON worker_messages(sender_email);
