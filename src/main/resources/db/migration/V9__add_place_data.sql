ALTER TABLE stops RENAME COLUMN name TO title;
ALTER TABLE trips DROP COLUMN destination;

ALTER TABLE trips
    ADD COLUMN place_name VARCHAR(255),
    ADD COLUMN google_place_id VARCHAR(255),
    ADD COLUMN place_latitude DOUBLE PRECISION,
    ADD COLUMN place_longitude DOUBLE PRECISION;

ALTER TABLE stops
    ADD COLUMN place_name VARCHAR(255),
    ADD COLUMN google_place_id VARCHAR(255),
    ADD COLUMN place_latitude DOUBLE PRECISION,
    ADD COLUMN place_longitude DOUBLE PRECISION,
    ADD COLUMN place_address TEXT,
    ADD COLUMN place_category VARCHAR(100);