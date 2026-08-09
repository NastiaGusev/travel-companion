CREATE TABLE stops
(
    id               BIGSERIAL PRIMARY KEY,
    itinerary_day_id BIGINT       NOT NULL REFERENCES itinerary_days (id) ON DELETE CASCADE,
    name             VARCHAR(255) NOT NULL,
    position         INT          NOT NULL,
    start_time       TIME,
    end_time         TIME,
    notes            TEXT,
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_stops_day_id ON stops (itinerary_day_id);