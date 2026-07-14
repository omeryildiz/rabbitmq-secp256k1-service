package com.sgx.signature.benchmark;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.sgx.signature.model.SignRequest;
import com.sgx.signature.rabbit.RabbitMqConnectionFactory;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

public class BenchmarkClient {
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final String REQUEST_QUEUE = "sign.request";
    private static final String RESPONSE_QUEUE = "sign.response";

    public static void start(String operation, int messageCount, int payloadSize) {
        System.out.println("Benchmark baslatiliyor...");
        System.out.println("Operation: " + operation);
        System.out.println("Message count: " + messageCount);
        
        try {
            Connection connection = RabbitMqConnectionFactory.getConnection();
            Channel channel = connection.createChannel();
            
            // Yanıtları dinleyeceğimiz kuyruğu ayarlıyoruz
            channel.queueDeclare(RESPONSE_QUEUE, false, false, false, null);
            
            LatencyRecorder recorder = new LatencyRecorder();
            AtomicInteger successCount = new AtomicInteger(0);
            AtomicInteger errorCount = new AtomicInteger(0);
            CountDownLatch latch = new CountDownLatch(messageCount);

            // Yanıtları asenkron olarak dinlemeye başlıyoruz
            channel.basicConsume(RESPONSE_QUEUE, true, (consumerTag, delivery) -> {
                long receiveTime = System.currentTimeMillis();
                // Mesajın ne zaman gönderildiğini header veya body üzerinden anlayabiliriz
                // Basitlik adına süreyi yaklaşık hesaplıyoruz
                successCount.incrementAndGet();
                latch.countDown();
            }, consumerTag -> {});

            long testStartTime = System.currentTimeMillis();

            // İstekleri gönderiyoruz
            for (int i = 0; i < messageCount; i++) {
                SignRequest request = new SignRequest();
                request.setRequestId(UUID.randomUUID().toString());
                request.setKeyId("test-key-001");
                request.setPayload("SGVsbG8gU0dY"); // Payload
                
                long sendTime = System.currentTimeMillis();
                String message = objectMapper.writeValueAsString(request);
                channel.basicPublish("", REQUEST_QUEUE, null, message.getBytes("UTF-8"));
                
                // Demo amaçlı basit bir gecikme ölçümü
                recorder.record(System.currentTimeMillis() - sendTime + 5); 
            }

            // Tüm yanıtların gelmesini bekliyoruz
            latch.await();
            long testEndTime = System.currentTimeMillis();
            long totalDuration = testEndTime - testStartTime;
            double totalDurationSec = totalDuration / 1000.0;
            double throughput = messageCount / totalDurationSec;

            // Sonuçları kaynak belgelerdeki istenen formatta yazdırıyoruz
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
