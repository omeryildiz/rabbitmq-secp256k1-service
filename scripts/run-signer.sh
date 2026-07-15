#!/bin/bash
set -euo pipefail

java -jar target/sgx-signature-rabbitmq-service-1.0-SNAPSHOT.jar \
  --mode=signer \
  --key-mode=file \
  --key-dir="${KEY_DIR:-/tmp/rabbitmq-secp256k1-service-keys}"
