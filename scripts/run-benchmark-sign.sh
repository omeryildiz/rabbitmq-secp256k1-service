#!/bin/bash
java -jar target/sgx-signature-rabbitmq-service-1.0-SNAPSHOT.jar --mode=benchmark-client --operation=sign --message-count=10000 --payload-size=32
