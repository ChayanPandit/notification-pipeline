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

-- One row per (notification × channel) delivery job
CREATE TABLE delivery_attempts (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    notification_id  UUID NOT NULL REFERENCES notifications(id),
    channel          VARCHAR(50) NOT NULL,  -- EMAIL | PUSH | WEBHOOK
    status           VARCHAR(50) NOT NULL,
    -- IN_PROGRESS | DELIVERED | FAILED | DEAD_LETTERED
    attempt_number   INT NOT NULL DEFAULT 1,
    next_retry_at    TIMESTAMPTZ,
    error_message    TEXT,
    duration_ms      INT,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT uq_notif_channel_attempt
       UNIQUE (notification_id, channel, attempt_number)
);

-- Immutable audit log — append only, never updated
CREATE TABLE delivery_audit_log (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    notification_id  UUID NOT NULL,
    channel          VARCHAR(50) NOT NULL,
    event            VARCHAR(100) NOT NULL,
    -- ATTEMPT_STARTED | DELIVERED | FAILED | RETRIED | DEAD_LETTERED
    attempt_number   INT NOT NULL,
    metadata         JSONB,               -- response code, error, latency
    occurred_at      TIMESTAMPTZ NOT NULL DEFAULT now()
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
CREATE INDEX idx_delivery_attempts_notification
    ON delivery_attempts(notification_id);
CREATE INDEX idx_delivery_attempts_status_retry
    ON delivery_attempts(status, next_retry_at)
    WHERE status = 'FAILED';
CREATE INDEX idx_audit_notification
    ON delivery_audit_log(notification_id, occurred_at);
CREATE INDEX idx_notifications_recipient
    ON notifications(recipient_id, created_at DESC);