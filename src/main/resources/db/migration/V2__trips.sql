CREATE TABLE trips
(
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT       NOT NULL REFERENCES users (id),
    title       VARCHAR(255) NOT NULL,
    destination VARCHAR(255),
    start_date  DATE,
    end_date    DATE,
    description TEXT,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_trips_user_id ON trips (user_id);