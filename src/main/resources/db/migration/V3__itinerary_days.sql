CREATE TABLE itinerary_days
(
    id         BIGSERIAL PRIMARY KEY,
    trip_id    BIGINT      NOT NULL REFERENCES trips (id) ON DELETE CASCADE,
    day_number INT         NOT NULL,
    day_date   DATE,
    notes      TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (trip_id, day_number)
);

CREATE INDEX idx_itinerary_days_trip_id ON itinerary_days (trip_id);

