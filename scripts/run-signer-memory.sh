#!/bin/bash
set -euo pipefail

java -jar target/sgx-signature-rabbitmq-service-1.0-SNAPSHOT.jar \
  --mode=signer \
  --key-mode=memory \
  --key-id="${KEY_ID:-enclave-key}"
