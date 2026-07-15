#!/bin/bash
set -euo pipefail

java -jar target/sgx-signature-rabbitmq-service-1.0-SNAPSHOT.jar \
  --mode=benchmark-client \
  --operation=sign \
  --message-count="${MESSAGE_COUNT:-10000}" \
  --payload-size="${PAYLOAD_SIZE:-32}" \
  --key-id="${KEY_ID:-test-key-001}"
