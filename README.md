# Notification Pipeline

Production-style event-driven notification pipeline built with Kotlin, Spring Boot, Kafka, and PostgreSQL.

The project accepts events over REST, stores them durably, publishes them asynchronously through an outbox, fans them out to channel-specific Kafka topics, and delivers them to email, push, and webhook channels with retry and DLQ handling.

## Architecture

```text
Client
  |
  | POST /api/v1/events
  v
Ingest API
  |
  |-- idempotency check
  |-- write notifications
  |-- write idempotency_keys
  |-- write outbox_events
  v
PostgreSQL
  ^
  |
Outbox Publisher
  |
  | publish
  v
Kafka: notifications.inbound
  |
  v
Event Router
  |
  |-- notifications.delivery.email
  |-- notifications.delivery.push
  |-- notifications.delivery.webhook
  v
Channel Workers
  |
  |-- consumer-side idempotency via channel_deliveries
  |-- delivery_attempts
  |-- delivery_audit_log
  |
  +--> success
  |
  +--> retry topic -> retry relay -> delivery topic
  |
  +--> DLQ
```

## Request Flow

1. Client sends an event to `POST /api/v1/events` with an `Idempotency-Key`.
2. The ingest service checks `idempotency_keys`.
3. If the key is new, the service stores `notifications`, `idempotency_keys`, and `outbox_events` in one DB transaction.
4. A scheduled outbox publisher reads pending outbox rows and publishes the event to Kafka topic `notifications.inbound`.
5. The router consumer reads `notifications.inbound` and fans the event out to channel topics based on the event type.
6. Each channel worker consumes its delivery topic and checks `channel_deliveries` before invoking the provider.
7. If the channel was already delivered, the worker skips the external send and acknowledges the Kafka message.
8. If the channel was not yet delivered, the worker performs the send and records attempt and audit state.
9. Failures are either retried through tiered retry topics or sent to DLQ.

## Delivery Guarantees

### API Idempotency

- Enforced through `idempotency_keys`
- Same client request does not create multiple notification rows

### DB -> Kafka Durability

- Enforced through `outbox_events`
- The API never commits notification state without also committing a pending Kafka publish record

### Consumer-Side Idempotency

- Enforced through `channel_deliveries`
- Kafka is treated as at-least-once transport
- A redelivered channel message does not trigger a duplicate external send after the channel is already marked `DELIVERED`

### Overall System Guarantee

- Ingest is idempotent
- Kafka processing is at-least-once
- External side effects are made effectively exactly-once per `(notification_id, channel)` by the Postgres idempotency barrier

## PostgreSQL Tables

### `notifications`

One row per accepted inbound notification event.

### `idempotency_keys`

Maps client idempotency keys to notification IDs.

### `outbox_events`

Stores pending Kafka publishes so DB commit and event publication are decoupled safely.

### `channel_deliveries`

Stores current per-channel delivery state for `(notification_id, channel)`.

### `delivery_attempts`

One row per actual delivery attempt.

### `delivery_audit_log`

Append-only timeline of delivery events such as `ATTEMPT_STARTED`, `RETRIED`, and `DELIVERED`.

## Kafka Topic Topology

### Inbound

- `notifications.inbound`
  - Partitions: 6
  - Retention: 7 days

### Delivery

- `notifications.delivery.email`
  - Partitions: 6
  - Retention: 7 days
- `notifications.delivery.push`
  - Partitions: 6
  - Retention: 7 days
- `notifications.delivery.webhook`
  - Partitions: 6
  - Retention: 7 days

### Retry

- `notifications.retry.email.tier1`
  - Partitions: 6
  - Retention: 1 day
- `notifications.retry.email.tier2`
  - Partitions: 6
  - Retention: 1 day
- `notifications.retry.email.tier3`
  - Partitions: 6
  - Retention: 1 day
- `notifications.retry.push.tier1`
  - Partitions: 6
  - Retention: 1 day
- `notifications.retry.push.tier2`
  - Partitions: 6
  - Retention: 1 day
- `notifications.retry.push.tier3`
  - Partitions: 6
  - Retention: 1 day
- `notifications.retry.webhook.tier1`
  - Partitions: 6
  - Retention: 1 day
- `notifications.retry.webhook.tier2`
  - Partitions: 6
  - Retention: 1 day
- `notifications.retry.webhook.tier3`
  - Partitions: 6
  - Retention: 1 day

### DLQ

- `notifications.dlq.email`
  - Partitions: 3
  - Retention: 14 days
- `notifications.dlq.push`
  - Partitions: 3
  - Retention: 14 days
- `notifications.dlq.webhook`
  - Partitions: 3
  - Retention: 14 days

## Retry Tier Table

| Attempt | Delay | Topic |
| --- | --- | --- |
| 1 | ~1s + jitter | `notifications.retry.<channel>.tier1` |
| 2 | ~10s + jitter | `notifications.retry.<channel>.tier2` |
| 3 | ~60s + jitter | `notifications.retry.<channel>.tier3` |
| 4+ | DLQ | `notifications.dlq.<channel>` |

Notes:

- Delay is carried in the message as `nextAttemptAt`
- Retry topics are consumed by relay workers, not directly by the channel workers
- Relay workers forward the event back to the delivery topic only when the retry is due

## DLQ Design

- One DLQ topic per channel
- DLQ messages preserve the original payload and channel context
- DLQ replay is exposed through admin endpoints:
  - `POST /api/v1/admin/dlq/replay?channel=EMAIL|PUSH|WEBHOOK`
  - `POST /api/v1/admin/dlq/replay/all`

## Webhook Channel

Webhook delivery is the most realistic channel in the project.

Implemented behavior:

- real HTTP call using Java `HttpClient`
- connect timeout and read timeout
- redirects are not followed
- HMAC-SHA256 signature over `timestamp + "." + body`
- headers:
  - `Idempotency-Key`
  - `X-Notif-Delivery-Id`
  - `X-Notif-Event-Type`
  - `X-Notif-Timestamp`
  - `X-Notif-Signature`

Response classification:

- `2xx`: success
- `408`, `425`, `429`, `5xx`: retryable
- `301`, `302`, `307`, `308`: non-retryable
- other `4xx`: non-retryable

Local testing is supported through:

- `POST /api/v1/webhook-sink/{recipientId}`

Use `?status=503` to simulate retryable failures and `?status=400` for terminal failures.

## Running Locally

### Start Infrastructure

```powershell
docker compose up -d
```

If Kafka topics were not created automatically:

```powershell
docker compose run --rm kafka-init /bin/sh /opt/kafka/scripts/create-topics.sh
```

### Run the App

```powershell
./gradlew bootRun
```

### Send One Event

```powershell
$headers = @{
  "Content-Type" = "application/json"
  "Idempotency-Key" = "demo-1"
}

$body = @{
  eventType = "ORDER_PLACED"
  recipientId = "user-123"
  payload = @{
    orderId = "ord-1"
    webhookUrl = "http://localhost:8080/api/v1/webhook-sink/user-123"
  }
} | ConvertTo-Json -Depth 5

Invoke-RestMethod `
  -Method POST `
  -Uri "http://localhost:8080/api/v1/events" `
  -Headers $headers `
  -Body $body
```

### Useful Verification Queries

```sql
select * from notifications order by created_at desc;
select * from outbox_events order by created_at desc;
select * from channel_deliveries order by created_at desc;
select * from delivery_attempts order by created_at desc;
select * from delivery_audit_log order by occurred_at desc;
```

## Load Testing

Current load test script is in:

- `load-tests/notification_load_test.js`

Planned target scenario:

- 500 events/sec
- 5% webhook failures to exercise retry and DLQ behavior

## What Changes at 10x Scale

- Move from single-broker local Kafka to a multi-broker managed setup
- Increase partition counts per topic based on per-channel throughput
- Replace simple retry relay behavior with a cleaner delayed-release scheduler or broker-native delay mechanism
- Add Redis as a fast-path dedupe cache while keeping Postgres as the durable delivery truth
- Separate webhook delivery into its own deployable worker pool if webhook traffic dominates
- Add topic provisioning through infra tooling instead of a startup shell script
- Add stronger dashboarding and alerting for outbox lag, retry backlog, and DLQ growth

## Project Status

Implemented:

- REST ingest
- API idempotency
- transactional outbox
- Kafka fan-out
- manual offset commit consumers
- consumer-side idempotency
- retry topics and DLQ
- retry-delay relay
- webhook HTTP delivery with request signing
- Docker Compose for local infra
- Prometheus and Grafana containers

Still worth doing later:

- tighter retry release implementation
- stronger observability dashboards and alerts
- exact k6 target scenario
- README diagrams as images
- kind/Kubernetes validation
