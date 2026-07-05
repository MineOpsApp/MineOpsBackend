ALTER TABLE sites ADD COLUMN minerals_produced VARCHAR(500);
ALTER TABLE sites ADD COLUMN production_capacity VARCHAR(255);
ALTER TABLE sites ADD COLUMN established_year INTEGER;
ALTER TABLE sites ADD COLUMN profile_description VARCHAR(1000);
ALTER TABLE sites ADD COLUMN contact_email VARCHAR(255);

CREATE TABLE forum_post (
    id BIGSERIAL PRIMARY KEY,
    category VARCHAR(50) NOT NULL,
    subforum VARCHAR(20) NOT NULL DEFAULT 'GENERAL',
    author_email VARCHAR(255) NOT NULL,
    author_name VARCHAR(255) NOT NULL,
    author_role VARCHAR(20) NOT NULL,
    title VARCHAR(255) NOT NULL,
    body VARCHAR(3000) NOT NULL,
    reply_count INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT now()
);
CREATE INDEX idx_forum_post_subforum ON forum_post(subforum);
CREATE INDEX idx_forum_post_category ON forum_post(category);

CREATE TABLE forum_reply (
    id BIGSERIAL PRIMARY KEY,
    post_id BIGINT NOT NULL REFERENCES forum_post(id),
    author_email VARCHAR(255) NOT NULL,
    author_name VARCHAR(255) NOT NULL,
    author_role VARCHAR(20) NOT NULL,
    body VARCHAR(2000) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT now()
);
CREATE INDEX idx_forum_reply_post ON forum_reply(post_id);

CREATE TABLE community_event (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    description VARCHAR(1000),
    event_type VARCHAR(30) NOT NULL,
    event_date TIMESTAMP NOT NULL,
    created_by_email VARCHAR(255) NOT NULL,
    created_by_name VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT now()
);
CREATE INDEX idx_event_date ON community_event(event_date);

CREATE TABLE job_posting (
    id BIGSERIAL PRIMARY KEY,
    site VARCHAR(255) NOT NULL,
    title VARCHAR(255) NOT NULL,
    description VARCHAR(1500) NOT NULL,
    posted_by_email VARCHAR(255) NOT NULL,
    posted_by_name VARCHAR(255) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    created_at TIMESTAMP NOT NULL DEFAULT now()
);
CREATE INDEX idx_job_status ON job_posting(status);

CREATE TABLE job_interest (
    id BIGSERIAL PRIMARY KEY,
    job_posting_id BIGINT NOT NULL REFERENCES job_posting(id),
    applicant_email VARCHAR(255) NOT NULL,
    applicant_name VARCHAR(255) NOT NULL,
    applicant_role VARCHAR(20) NOT NULL,
    message VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT now()
);
CREATE INDEX idx_job_interest_posting ON job_interest(job_posting_id);

CREATE TABLE marketplace_rating (
    id BIGSERIAL PRIMARY KEY,
    transaction_id BIGINT NOT NULL REFERENCES marketplace_transaction(id),
    rater_email VARCHAR(255) NOT NULL,
    rater_role VARCHAR(20) NOT NULL,
    reliability INT NOT NULL,
    communication INT NOT NULL,
    product_quality INT,
    listing_accuracy INT,
    comment VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    UNIQUE(transaction_id, rater_email)
);

CREATE TABLE transaction_dispute (
    id BIGSERIAL PRIMARY KEY,
    transaction_id BIGINT NOT NULL REFERENCES marketplace_transaction(id),
    raised_by_email VARCHAR(255) NOT NULL,
    raised_by_role VARCHAR(20) NOT NULL,
    reason VARCHAR(1000) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    resolution_notes VARCHAR(1000),
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    resolved_at TIMESTAMP
);
CREATE INDEX idx_dispute_transaction ON transaction_dispute(transaction_id);
