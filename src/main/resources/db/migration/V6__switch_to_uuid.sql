DROP TABLE IF EXISTS stops;
DROP TABLE IF EXISTS itinerary_days;
DROP TABLE IF EXISTS trips;
DROP TABLE IF EXISTS users;

CREATE TABLE users
(
    id            UUID PRIMARY KEY      DEFAULT gen_random_uuid(),
    email         VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    display_name  VARCHAR(255),
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE trips
(
    id          UUID PRIMARY KEY      DEFAULT gen_random_uuid(),
    user_id     UUID         NOT NULL REFERENCES users (id),
    title       VARCHAR(255) NOT NULL,
    destination VARCHAR(255),
    start_date  DATE,
    end_date    DATE,
    description TEXT,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX idx_trips_user_id ON trips (user_id);

CREATE TABLE itinerary_days
(
    id         UUID PRIMARY KEY     DEFAULT gen_random_uuid(),
    trip_id    UUID        NOT NULL REFERENCES trips (id) ON DELETE CASCADE,
    day_number INT         NOT NULL,
    notes      TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (trip_id, day_number)
);
CREATE INDEX idx_itinerary_days_trip_id ON itinerary_days (trip_id);

CREATE TABLE stops
(
    id               UUID PRIMARY KEY      DEFAULT gen_random_uuid(),
    itinerary_day_id UUID         NOT NULL REFERENCES itinerary_days (id) ON DELETE CASCADE,
    name             VARCHAR(255) NOT NULL,
    position         INT          NOT NULL,
    start_time       TIME,
    end_time         TIME,
    notes            TEXT,
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX idx_stops_day_id ON stops (itinerary_day_id);