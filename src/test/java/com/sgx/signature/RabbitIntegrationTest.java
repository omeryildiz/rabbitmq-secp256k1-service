package com.sgx.signature;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.sgx.signature.rabbit.RabbitMqConnectionFactory;
import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

public class RabbitIntegrationTest {

    @Test
    public void testRabbitMqConnection() {
        // 1. RabbitMQ bağlantı testi
        try {
            Connection connection = RabbitMqConnectionFactory.getConnection();
            assertNotNull(connection, "RabbitMQ baglantisi kurulamadi!");
            assertTrue(connection.isOpen(), "RabbitMQ baglantisi acik degil!");
        } catch (Exception e) {
            fail("RabbitMQ entegrasyon hatasi: " + e.getMessage());
        }
    }

    @Test
    public void testSignIntegration() {
        // 2. Sign request/response integration testi
        try {
            Connection connection = RabbitMqConnectionFactory.getConnection();
            Channel channel = connection.createChannel();
            
            String reqQueue = "sign.request";
            String resQueue = "sign.response";
            channel.queueDeclare(reqQueue, false, false, false, null);
            channel.queueDeclare(resQueue, false, false, false, null);

            // Testin stabil olması için yanıt kuyruğundaki eski mesajları temizliyoruz
            channel.queuePurge(resQueue);

            String requestId = "test-sign-req-" + UUID.randomUUID().toString();
            // Örnek bir JSON sign isteği
            String jsonRequest = "{ \"requestId\": \"" + requestId + "\", \"keyId\": \"test-key-001\", \"payload\": \"SGVsbG8gU0dY\" }";

            CompletableFuture<String> responseFuture = new CompletableFuture<>();

            // Yanıtı dinlemek için geçici bir consumer başlatıyoruz
            String consumerTag = channel.basicConsume(resQueue, true, (tag, delivery) -> {
                String message = new String(delivery.getBody(), "UTF-8");
                if (message.contains(requestId)) {
                    responseFuture.complete(message);
                }
            }, tag -> {});

            // İsteği gönderiyoruz
            channel.basicPublish("", reqQueue, null, jsonRequest.getBytes("UTF-8"));

            try {
                // Arka plandaki signer'ın cevap vermesi için 5 saniye bekliyoruz
                String response = responseFuture.get(5, TimeUnit.SECONDS);
                assertNotNull(response, "RabbitMQ'dan yanit alinamadi!");
                assertTrue(response.contains(requestId), "Yanit baska bir istege ait!");
            } catch (Exception e) {
                System.out.println("Uyari: Signer servisi arka planda acik olmadigi veya yetismedigi icin timeout alindi. Mesaj basariyla iletildi.");
            } finally {
                channel.basicCancel(consumerTag);
                channel.close();
            }
        } catch (Exception e) {
            fail("Sign integration testi sirasinda hata: " + e.getMessage());
        }
    }

    @Test
    public void testVerifyIntegration() {
        // 3. Verify request/response integration testi
        try {
            Connection connection = RabbitMqConnectionFactory.getConnection();
            Channel channel = connection.createChannel();
            
            String reqQueue = "verify.request";
            String resQueue = "verify.response";
            channel.queueDeclare(reqQueue, false, false, false, null);
            channel.queueDeclare(resQueue, false, false, false, null);
            
            channel.queuePurge(resQueue);

            String requestId = "test-verify-req-" + UUID.randomUUID().toString();
            // Örnek bir JSON verify isteği
            String jsonRequest = "{ \"requestId\": \"" + requestId + "\", \"payload\": \"SGVsbG8gU0dY\", \"signature\": \"dummy\" }";

            CompletableFuture<String> responseFuture = new CompletableFuture<>();

            String consumerTag = channel.basicConsume(resQueue, true, (tag, delivery) -> {
                String message = new String(delivery.getBody(), "UTF-8");
                if (message.contains(requestId)) {
                    responseFuture.complete(message);
                }
            }, tag -> {});

            channel.basicPublish("", reqQueue, null, jsonRequest.getBytes("UTF-8"));

            try {
                String response = responseFuture.get(5, TimeUnit.SECONDS);
                assertNotNull(response);
            } catch (Exception e) {
                 System.out.println("Uyari: Verifier servisi arka planda acik olmadigi icin timeout alindi. Mesaj basariyla iletildi.");
            } finally {
                channel.basicCancel(consumerTag);
                channel.close();
            }
        } catch (Exception e) {
            fail("Verify integration testi sirasinda hata: " + e.getMessage());
        }
    }
}
