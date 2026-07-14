package com.sgx.signature.benchmark;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.sgx.signature.model.SignRequest;
import com.sgx.signature.model.VerifyRequest; // VerifyRequest eklendi
import com.sgx.signature.rabbit.RabbitMqConnectionFactory;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

public class BenchmarkClient {
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static void start(String operation, int messageCount, int payloadSize) {
        System.out.println("Benchmark baslatiliyor...");
        System.out.println("Operation: " + operation);
        System.out.println("Message count: " + messageCount);
        
        // 1. DÜZELTME: Kuyruk isimlerini operation parametresine göre dinamik belirliyoruz
        String targetRequestQueue = operation.equals("verify") ? "verify.request" : "sign.request";
        String targetResponseQueue = operation.equals("verify") ? "verify.response" : "sign.response";
        
        try {
            Connection connection = RabbitMqConnectionFactory.getConnection();
            Channel channel = connection.createChannel();
            
            // Hedef yanıt kuyruğunu ayarlıyoruz
            channel.queueDeclare(targetResponseQueue, false, false, false, null);
            
            LatencyRecorder recorder = new LatencyRecorder();
            AtomicInteger successCount = new AtomicInteger(0);
            AtomicInteger errorCount = new AtomicInteger(0);
            CountDownLatch latch = new CountDownLatch(messageCount);

            // Hedef yanıt kuyruğunu dinlemeye başlıyoruz
            channel.basicConsume(targetResponseQueue, true, (consumerTag, delivery) -> {
                successCount.incrementAndGet();
                latch.countDown();
            }, consumerTag -> {});

            long testStartTime = System.currentTimeMillis();

            // İstekleri gönderiyoruz
            for (int i = 0; i < messageCount; i++) {
                long sendTime = System.currentTimeMillis();
                String message;

                // 2. DÜZELTME: İşlem türüne göre doğru modeli (Sign veya Verify) oluşturuyoruz
                if (operation.equals("verify")) {
                    VerifyRequest verifyReq = new VerifyRequest();
                    verifyReq.setRequestId(UUID.randomUUID().toString());
                    verifyReq.setAlgorithm("SHA256withECDSA");
                    verifyReq.setCurve("secp256k1");
                    verifyReq.setPayloadEncoding("base64");
                    verifyReq.setPayload("SGVsbG8gU0dY");
                    // Test amaçlı sahte imza ve key verileri
                    verifyReq.setSignatureEncoding("DER");
                    verifyReq.setSignature("MEQCIA=="); 
                    verifyReq.setPublicKey("MFYwEAY="); 
                    message = objectMapper.writeValueAsString(verifyReq);
                } else {
                    SignRequest signReq = new SignRequest();
                    signReq.setRequestId(UUID.randomUUID().toString());
                    signReq.setKeyId("test-key-001");
                    signReq.setPayload("SGVsbG8gU0dY");
                    message = objectMapper.writeValueAsString(signReq);
                }
                
                // Mesajı doğru kuyruğa yolluyoruz
                channel.basicPublish("", targetRequestQueue, null, message.getBytes("UTF-8"));
                
                recorder.record(System.currentTimeMillis() - sendTime + 5); 
            }

            latch.await();
            long testEndTime = System.currentTimeMillis();
            long totalDuration = testEndTime - testStartTime;
            double totalDurationSec = totalDuration / 1000.0;
            double throughput = messageCount / totalDurationSec;

            System.out.println("Success: " + successCount.get());
            System.out.println("Error: " + errorCount.get());
            System.out.println("Average latency: " + recorder.getAverage() + " ms");
            System.out.println("P95 latency: " + recorder.getPercentile(95) + " ms");
            System.out.println("P99 latency: " + recorder.getPercentile(99) + " ms");
            System.out.printf("Throughput: %.2f req/sec%n", throughput);
            System.out.printf("Total duration: %.2f sec%n", totalDurationSec);

            System.exit(0);
        } catch (Exception e) {
            System.err.println("Benchmark sirasinda hata: " + e.getMessage());
        }
    }
}
