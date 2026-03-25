CREATE TABLE delivery_attempts (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    notification_id  UUID NOT NULL REFERENCES notifications(id),
    channel          VARCHAR(50) NOT NULL,
    status           VARCHAR(50) NOT NULL DEFAULT 'IN_PROGRESS',
    attempt_number   INT NOT NULL DEFAULT 1,
    next_retry_at    TIMESTAMPTZ,
    error_message    TEXT,
    duration_ms      BIGINT,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_notif_channel_attempt UNIQUE (notification_id, channel, attempt_number)
);

CREATE TABLE delivery_audit_log (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    notification_id  UUID NOT NULL,
    channel          VARCHAR(50) NOT NULL,
    event            VARCHAR(100) NOT NULL,
    attempt_number   INT NOT NULL,
    metadata         JSONB,
    occurred_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_delivery_attempts_notification
    ON delivery_attempts(notification_id);
CREATE INDEX idx_audit_notification
    ON delivery_audit_log(notification_id, occurred_at);