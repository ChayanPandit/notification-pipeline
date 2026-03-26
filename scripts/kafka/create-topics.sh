#!/bin/sh
set -eu

BOOTSTRAP_SERVER="${BOOTSTRAP_SERVER:-kafka:29092}"

create_topic() {
  topic="$1"
  partitions="$2"
  retention_ms="${3:-}"

  echo "Creating topic: ${topic}"
  if [ -n "${retention_ms}" ]; then
    kafka-topics --bootstrap-server "${BOOTSTRAP_SERVER}" \
      --create \
      --if-not-exists \
      --topic "${topic}" \
      --partitions "${partitions}" \
      --replication-factor 1 \
      --config retention.ms="${retention_ms}"
  else
    kafka-topics --bootstrap-server "${BOOTSTRAP_SERVER}" \
      --create \
      --if-not-exists \
      --topic "${topic}" \
      --partitions "${partitions}" \
      --replication-factor 1
  fi
}

create_topic "notifications.inbound" 6 604800000

create_topic "notifications.delivery.email" 6 604800000
create_topic "notifications.delivery.push" 6 604800000
create_topic "notifications.delivery.webhook" 6 604800000

create_topic "notifications.retry.email.tier1" 6 86400000
create_topic "notifications.retry.email.tier2" 6 86400000
create_topic "notifications.retry.email.tier3" 6 86400000

create_topic "notifications.retry.push.tier1" 6 86400000
create_topic "notifications.retry.push.tier2" 6 86400000
create_topic "notifications.retry.push.tier3" 6 86400000

create_topic "notifications.retry.webhook.tier1" 6 86400000
create_topic "notifications.retry.webhook.tier2" 6 86400000
create_topic "notifications.retry.webhook.tier3" 6 86400000

create_topic "notifications.dlq.email" 3 1209600000
create_topic "notifications.dlq.push" 3 1209600000
create_topic "notifications.dlq.webhook" 3 1209600000

echo "Kafka topic bootstrap complete"
