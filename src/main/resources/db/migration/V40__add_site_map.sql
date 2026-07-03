CREATE TABLE site_map (
    id BIGSERIAL PRIMARY KEY,
    site VARCHAR(255) NOT NULL UNIQUE,
    image_data TEXT NOT NULL,
    uploaded_by VARCHAR(255) NOT NULL,
    uploaded_at TIMESTAMP NOT NULL DEFAULT now()
);

ALTER TABLE danger_zones ADD COLUMN polygon_points TEXT;
