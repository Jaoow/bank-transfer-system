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