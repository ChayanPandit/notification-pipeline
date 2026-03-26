-- Core notification record (one per inbound event)
CREATE TABLE notifications (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    idempotency_key VARCHAR(255) UNIQUE NOT NULL,
    event_type      VARCHAR(100) NOT NULL,
    recipient_id    VARCHAR(255) NOT NULL,
    payload         JSONB NOT NULL,
    status          VARCHAR(50) NOT NULL DEFAULT 'RECEIVED',
    -- RECEIVED | ROUTING_COMPLETE | PARTIALLY_DELIVERED
    -- FULLY_DELIVERED | DEAD_LETTERED
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Idempotency key store (could fold into notifications, kept separate
-- for faster lookup and TTL-based cleanup)
CREATE TABLE idempotency_keys (
    key             VARCHAR(255) PRIMARY KEY,
    notification_id UUID NOT NULL REFERENCES notifications(id),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    expires_at      TIMESTAMPTZ NOT NULL DEFAULT now() + INTERVAL '24 hours'
);

-- Indexes
CREATE INDEX idx_notifications_recipient
    ON notifications(recipient_id, created_at DESC);
