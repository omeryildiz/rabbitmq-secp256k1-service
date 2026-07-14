package com.sgx.signature.rabbit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.DeliverCallback;
import com.sgx.signature.crypto.Secp256k1Signer;
import com.sgx.signature.model.SignRequest;
import com.sgx.signature.model.SignResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Paths;

public class SignRequestConsumer {
    private static final Logger log = LoggerFactory.getLogger(java.lang.invoke.MethodHandles.lookup().lookupClass());
    private static final Logger logger = LoggerFactory.getLogger(SignRequestConsumer.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final String REQUEST_QUEUE = "sign.request";
    private static final String RESPONSE_QUEUE = "sign.response";

    public static void start() {
        try {
            Connection connection = RabbitMqConnectionFactory.getConnection();
            Channel channel = connection.createChannel();
            log.info("Kuyruk (queue) declare edildi.");
            channel.queueDeclare(REQUEST_QUEUE, false, false, false, null);
            logger.info("Kuyruk declare edildi: {}", REQUEST_QUEUE);

            DeliverCallback deliverCallback = (consumerTag, delivery) -> {
                long startTime = System.nanoTime();
                String message = new String(delivery.getBody(), "UTF-8");
                SignResponse response = new SignResponse();
                
                try {
                    SignRequest request = objectMapper.readValue(message, SignRequest.class);
                    response.setRequestId(request.getRequestId());
                    
                    String privateKeyPath = "keys/" + request.getKeyId() + "-private.pem";
                    String publicKeyPath = "keys/" + request.getKeyId() + "-public.pem";
                    
                    if (!Files.exists(Paths.get(privateKeyPath))) {
                        throw new RuntimeException("Key not found: " + request.getKeyId());
                    }

                    // Kripto sınıfımızı çağırıp imzalıyoruz
                    String signature = Secp256k1Signer.signPayload(request.getPayload(), privateKeyPath);
                    
                    response.setStatus("OK");
                    response.setAlgorithm("SHA256withECDSA");
                    response.setCurve("secp256k1");
                    response.setSignatureEncoding("DER");
                    response.setSignature(signature);
                    
                    // Public key'i PEM formatından temizleyip base64 olarak ekliyoruz
                    String pubKeyContent = new String(Files.readAllBytes(Paths.get(publicKeyPath)))
                            .replace("-----BEGIN PUBLIC KEY-----", "")
                            .replace("-----END PUBLIC KEY-----", "")
                            .replaceAll("\\s", "");
                    response.setPublicKey(pubKeyContent);

                } catch (Exception e) {
            log.error("Islem sirasinda hata olustu: ", e);
                    logger.error("İmzalama hatası: {}", e.getMessage());
                    response.setStatus("ERROR");
                    response.setErrorCode("SIGN_ERROR");
                    response.setErrorMessage(e.getMessage());
                } finally {
                    long endTime = System.nanoTime();
                    // İşlem süresini mikrosaniye cinsinden hesaplayıp ekliyoruz
                    response.setProcessingTimeMicros((endTime - startTime) / 1000);
                    ResponsePublisher.publish(RESPONSE_QUEUE, response);
                }
            };

            log.info("Consumer basladi ve kuyruk dinleniyor.");
            channel.basicConsume(REQUEST_QUEUE, true, deliverCallback, consumerTag -> {});
            logger.info("Signer consumer başladı. {} dinleniyor...", REQUEST_QUEUE);
            
        } catch (Exception e) {
            log.error("Islem sirasinda hata olustu: ", e);
            logger.error("SignRequestConsumer baslatilamadi: {}", e.getMessage());
        }
    }
}
