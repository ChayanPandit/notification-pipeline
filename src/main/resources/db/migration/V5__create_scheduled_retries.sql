CREATE TABLE scheduled_retries (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    notification_id UUID NOT NULL REFERENCES notifications(id),
    channel        VARCHAR(50) NOT NULL,
    source_topic   VARCHAR(255) NOT NULL,
    delivery_topic VARCHAR(255) NOT NULL,
    message_key    VARCHAR(255) NOT NULL,
    due_at         TIMESTAMPTZ NOT NULL,
    event_payload  JSONB NOT NULL,
    status         VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    published_at   TIMESTAMPTZ,
    last_error     TEXT
);

CREATE INDEX idx_scheduled_retries_due_status
    ON scheduled_retries(status, due_at);
