CREATE TABLE trip_collaborators
(
    id         UUID PRIMARY KEY     DEFAULT gen_random_uuid(),
    trip_id    UUID        NOT NULL REFERENCES trips (id) ON DELETE CASCADE,
    user_id    UUID        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    role       VARCHAR(20) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (trip_id, user_id)
);