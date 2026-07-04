ALTER TABLE app_users ADD COLUMN business_name VARCHAR(255);
ALTER TABLE app_users ADD COLUMN buyer_verification_status VARCHAR(20);
ALTER TABLE app_users ADD COLUMN verification_document TEXT;

CREATE TABLE mineral_listing (
    id BIGSERIAL PRIMARY KEY,
    site VARCHAR(255) NOT NULL,
    mineral_type VARCHAR(100) NOT NULL,
    quantity DECIMAL(14,3) NOT NULL,
    unit VARCHAR(20) NOT NULL,
    grade VARCHAR(100),
    asking_price DECIMAL(14,2) NOT NULL,
    location VARCHAR(255),
    available_from DATE,
    min_order_quantity DECIMAL(14,3),
    photo_data TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_by VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT now()
);
CREATE INDEX idx_listing_site ON mineral_listing(site);
CREATE INDEX idx_listing_status ON mineral_listing(status);

CREATE TABLE marketplace_offer (
    id BIGSERIAL PRIMARY KEY,
    listing_id BIGINT NOT NULL REFERENCES mineral_listing(id),
    parent_offer_id BIGINT REFERENCES marketplace_offer(id),
    buyer_email VARCHAR(255) NOT NULL,
    buyer_name VARCHAR(255) NOT NULL,
    offer_price DECIMAL(14,2) NOT NULL,
    offer_quantity DECIMAL(14,3) NOT NULL,
    message VARCHAR(500),
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    responded_at TIMESTAMP,
    responded_by VARCHAR(255)
);
CREATE INDEX idx_offer_listing ON marketplace_offer(listing_id);
CREATE INDEX idx_offer_buyer ON marketplace_offer(buyer_email);

CREATE TABLE marketplace_transaction (
    id BIGSERIAL PRIMARY KEY,
    listing_id BIGINT NOT NULL REFERENCES mineral_listing(id),
    offer_id BIGINT NOT NULL REFERENCES marketplace_offer(id),
    site VARCHAR(255) NOT NULL,
    buyer_email VARCHAR(255) NOT NULL,
    buyer_name VARCHAR(255) NOT NULL,
    mineral_type VARCHAR(100) NOT NULL,
    quantity DECIMAL(14,3) NOT NULL,
    agreed_price DECIMAL(14,2) NOT NULL,
    batch_status VARCHAR(20) NOT NULL DEFAULT 'PREPARING',
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_by VARCHAR(255),
    updated_at TIMESTAMP
);
CREATE INDEX idx_transaction_site ON marketplace_transaction(site);
CREATE INDEX idx_transaction_buyer ON marketplace_transaction(buyer_email);
