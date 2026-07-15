#!/bin/bash
set -euo pipefail

java -jar target/sgx-signature-rabbitmq-service-1.0-SNAPSHOT.jar \
  --mode=keygen \
  --key-id="${KEY_ID:-test-key-001}" \
  --key-dir="${KEY_DIR:-/tmp/rabbitmq-secp256k1-service-keys}"
