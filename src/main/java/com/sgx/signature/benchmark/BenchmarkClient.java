package com.sgx.signature.benchmark;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.sgx.signature.crypto.InMemorySigningKeyProvider;
import com.sgx.signature.model.SignRequest;
import com.sgx.signature.model.VerifyRequest;
import com.sgx.signature.rabbit.RabbitMqConnectionFactory;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public final class BenchmarkClient {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final long TIMEOUT_MINUTES = 10;

    private BenchmarkClient() {
    }

    public static void start(String operation, int messageCount, int payloadSize, String keyId) {
        validateArguments(operation, messageCount, payloadSize);
        String requestQueue = operation.equals("verify") ? "verify.request" : "sign.request";
        String responseQueue = operation.equals("verify") ? "verify.response" : "sign.response";

        try (Connection connection = RabbitMqConnectionFactory.getConnection();
             Channel channel = connection.createChannel()) {
            channel.queueDeclare(requestQueue, false, false, false, null);
            channel.queueDeclare(responseQueue, false, false, false, null);
            channel.queuePurge(responseQueue);

            LatencyRecorder recorder = new LatencyRecorder();
            AtomicInteger successCount = new AtomicInteger();
            AtomicInteger errorCount = new AtomicInteger();
            CountDownLatch latch = new CountDownLatch(messageCount);
            ConcurrentHashMap<String, Long> sentAtNanos = new ConcurrentHashMap<>();

            channel.basicConsume(responseQueue, true, (consumerTag, delivery) -> {
                JsonNode response = OBJECT_MAPPER.readTree(delivery.getBody());
                String requestId = response.path("requestId").asText();
                Long startNanos = sentAtNanos.remove(requestId);
                if (startNanos == null) {
                    return;
                }
                recorder.recordMicros((System.nanoTime() - startNanos) / 1_000);
                if ("OK".equals(response.path("status").asText())) {
                    successCount.incrementAndGet();
                } else {
                    errorCount.incrementAndGet();
                }
                latch.countDown();
            }, consumerTag -> { });

            byte[] payloadBytes = new byte[payloadSize];
            new SecureRandom().nextBytes(payloadBytes);
            String payloadBase64 = Base64.getEncoder().encodeToString(payloadBytes);

            String verifySignature = null;
            String verifyPublicKey = null;
            if (operation.equals("verify")) {
                InMemorySigningKeyProvider fixture = new InMemorySigningKeyProvider("benchmark-key");
                verifySignature = fixture.sign("benchmark-key", payloadBase64);
                verifyPublicKey = fixture.publicKeyBase64("benchmark-key");
            }

            System.out.printf("Benchmark: operation=%s messages=%d payload=%d bytes keyId=%s%n",
                    operation, messageCount, payloadSize, keyId);
            long testStartNanos = System.nanoTime();
            for (int i = 0; i < messageCount; i++) {
                String requestId = UUID.randomUUID().toString();
                byte[] message = operation.equals("verify")
                        ? verifyRequest(requestId, payloadBase64, verifySignature, verifyPublicKey)
                        : signRequest(requestId, keyId, payloadBase64);
                sentAtNanos.put(requestId, System.nanoTime());
                channel.basicPublish("", requestQueue, null, message);
            }

            if (!latch.await(TIMEOUT_MINUTES, TimeUnit.MINUTES)) {
                throw new IllegalStateException("Benchmark zaman aşımına uğradı; eksik yanıt=" + latch.getCount());
            }
            double totalSeconds = (System.nanoTime() - testStartNanos) / 1_000_000_000.0;
            double throughput = messageCount / totalSeconds;

            System.out.println("Success: " + successCount.get());
            System.out.println("Error: " + errorCount.get());
            System.out.printf("Average latency: %.3f ms%n", recorder.getAverageMillis());
            System.out.printf("P95 latency: %.3f ms%n", recorder.getPercentileMillis(95));
            System.out.printf("P99 latency: %.3f ms%n", recorder.getPercentileMillis(99));
            System.out.printf("Throughput: %.2f req/sec%n", throughput);
            System.out.printf("Total duration: %.3f sec%n", totalSeconds);

            if (errorCount.get() > 0) {
                throw new IllegalStateException(errorCount.get() + " istek hata ile sonuçlandı");
            }
        } catch (Exception e) {
            throw new IllegalStateException("Benchmark başarısız: " + e.getMessage(), e);
        }
    }

    private static byte[] signRequest(String requestId, String keyId, String payload) throws Exception {
        SignRequest request = new SignRequest();
        request.setRequestId(requestId);
        request.setKeyId(keyId);
        request.setAlgorithm("SHA256withECDSA");
        request.setCurve("secp256k1");
        request.setPayloadEncoding("base64");
        request.setPayload(payload);
        return OBJECT_MAPPER.writeValueAsString(request).getBytes(StandardCharsets.UTF_8);
    }

    private static byte[] verifyRequest(String requestId, String payload, String signature, String publicKey)
            throws Exception {
        VerifyRequest request = new VerifyRequest();
        request.setRequestId(requestId);
        request.setAlgorithm("SHA256withECDSA");
        request.setCurve("secp256k1");
        request.setPayloadEncoding("base64");
        request.setPayload(payload);
        request.setSignatureEncoding("DER");
        request.setSignature(signature);
        request.setPublicKey(publicKey);
        return OBJECT_MAPPER.writeValueAsString(request).getBytes(StandardCharsets.UTF_8);
    }

    private static void validateArguments(String operation, int messageCount, int payloadSize) {
        if (!operation.equals("sign") && !operation.equals("verify")) {
            throw new IllegalArgumentException("operation 'sign' veya 'verify' olmalıdır");
        }
        if (messageCount < 1 || payloadSize < 1) {
            throw new IllegalArgumentException("message-count ve payload-size pozitif olmalıdır");
        }
    }
}
