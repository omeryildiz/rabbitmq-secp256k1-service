package com.sgx.signature.rabbit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.DeliverCallback;
import com.sgx.signature.crypto.Secp256k1Verifier;
import com.sgx.signature.model.VerifyRequest;
import com.sgx.signature.model.VerifyResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VerifyRequestConsumer {
    private static final Logger log = LoggerFactory.getLogger(java.lang.invoke.MethodHandles.lookup().lookupClass());
    private static final Logger logger = LoggerFactory.getLogger(VerifyRequestConsumer.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final String REQUEST_QUEUE = "verify.request";
    private static final String RESPONSE_QUEUE = "verify.response";

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
                VerifyResponse response = new VerifyResponse();
                
                try {
                    VerifyRequest request = objectMapper.readValue(message, VerifyRequest.class);
                    response.setRequestId(request.getRequestId());
                    
                    // Kripto sınıfımızı çağırıp doğrulamayı yapıyoruz
                    boolean isValid = Secp256k1Verifier.verifySignature(
                        request.getPayload(), 
                        request.getSignature(), 
                        request.getPublicKey()
                    );
                    
                    response.setStatus("OK");
                    response.setValid(isValid);
                } catch (Exception e) {
            log.error("Islem sirasinda hata olustu: ", e);
                    logger.error("Doğrulama hatası: {}", e.getMessage());
                    response.setStatus("ERROR");
                    response.setValid(false);
                    response.setErrorCode("VERIFY_ERROR");
                    response.setErrorMessage(e.getMessage());
                } finally {
                    long endTime = System.nanoTime();
                    response.setProcessingTimeMicros((endTime - startTime) / 1000);
                    ResponsePublisher.publish(RESPONSE_QUEUE, response);
                }
            };

            log.info("Consumer basladi ve kuyruk dinleniyor.");
            channel.basicConsume(REQUEST_QUEUE, true, deliverCallback, consumerTag -> {});
            logger.info("Verifier consumer başladı. {} dinleniyor...", REQUEST_QUEUE);
            
        } catch (Exception e) {
            log.error("Islem sirasinda hata olustu: ", e);
            logger.error("VerifyRequestConsumer baslatilamadi: {}", e.getMessage());
        }
    }
}
