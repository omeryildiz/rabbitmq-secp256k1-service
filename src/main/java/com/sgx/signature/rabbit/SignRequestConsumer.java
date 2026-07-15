package com.sgx.signature.rabbit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.DeliverCallback;
import com.sgx.signature.crypto.SigningKeyProvider;
import com.sgx.signature.model.SignRequest;
import com.sgx.signature.model.SignResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SignRequestConsumer {
    private static final Logger log = LoggerFactory.getLogger(java.lang.invoke.MethodHandles.lookup().lookupClass());
    private static final Logger logger = LoggerFactory.getLogger(SignRequestConsumer.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final String REQUEST_QUEUE = "sign.request";
    private static final String RESPONSE_QUEUE = "sign.response";

    public static void start(SigningKeyProvider keyProvider) {
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
                    
                    String signature = keyProvider.sign(request.getKeyId(), request.getPayload());
                    
                    response.setStatus("OK");
                    response.setAlgorithm("SHA256withECDSA");
                    response.setCurve("secp256k1");
                    response.setSignatureEncoding("DER");
                    response.setSignature(signature);
                    
                    response.setPublicKey(keyProvider.publicKeyBase64(request.getKeyId()));

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
            logger.info("Signer consumer başladı. {} dinleniyor; anahtar kaynağı={}", REQUEST_QUEUE,
                    keyProvider.description());
            
        } catch (Exception e) {
            log.error("Islem sirasinda hata olustu: ", e);
            logger.error("SignRequestConsumer baslatilamadi: {}", e.getMessage());
        }
    }
}
