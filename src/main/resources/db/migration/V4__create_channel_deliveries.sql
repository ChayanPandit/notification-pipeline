CREATE TABLE channel_deliveries (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    notification_id     UUID NOT NULL REFERENCES notifications(id),
    channel             VARCHAR(50) NOT NULL,
    status              VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    last_attempt_number INT NOT NULL DEFAULT 0,
    delivered_at        TIMESTAMPTZ,
    last_error          TEXT,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_channel_delivery UNIQUE (notification_id, channel)
);

CREATE INDEX idx_channel_deliveries_notification
    ON channel_deliveries(notification_id);
