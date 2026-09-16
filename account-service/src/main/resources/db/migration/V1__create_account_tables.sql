CREATE TABLE accounts
(
    id      UUID PRIMARY KEY,
    balance NUMERIC(19, 4) NOT NULL,
    version BIGINT         NOT NULL DEFAULT 0
);

CREATE TABLE processed_events
(
    idempotency_key VARCHAR(100) PRIMARY KEY,
    event_type      VARCHAR(50) NOT NULL,
    processed_at    TIMESTAMP   NOT NULL DEFAULT now()
);

CREATE TABLE account_events
(
    id          UUID PRIMARY KEY,
    account_id  UUID           NOT NULL,
    event_type  VARCHAR(50)    NOT NULL,
    amount      NUMERIC(19, 4) NOT NULL,
    created_at  TIMESTAMP      NOT NULL DEFAULT now()
);

CREATE TABLE outbox_events
(
    id         UUID PRIMARY KEY,
    topic      VARCHAR(100) NOT NULL,
    event_key  VARCHAR(100),
    payload    TEXT         NOT NULL,
    published  BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP    NOT NULL DEFAULT now()
);

-- Seed accounts for demo / testing
INSERT INTO accounts (id, balance, version) VALUES
    ('a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11', 1000.0000, 0),
    ('b0eebc99-9c0b-4ef8-bb6d-6bb9bd380a22', 500.0000, 0)
ON CONFLICT (id) DO NOTHING;
