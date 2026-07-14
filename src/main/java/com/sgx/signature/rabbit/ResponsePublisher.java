package com.sgx.signature.rabbit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ResponsePublisher {
    private static final Logger log = LoggerFactory.getLogger(java.lang.invoke.MethodHandles.lookup().lookupClass());
    private static final Logger logger = LoggerFactory.getLogger(ResponsePublisher.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static void publish(String queueName, Object response) {
        try {
            Connection connection = RabbitMqConnectionFactory.getConnection();
            Channel channel = connection.createChannel();
            
            // Kuyruk yoksa oluşturuluyor
            log.info("Kuyruk (queue) declare edildi.");
            channel.queueDeclare(queueName, false, false, false, null);
            logger.info("Kuyruk declare edildi: {}", queueName);

            // Java nesnesini (SignResponse/VerifyResponse) JSON formatına çeviriyoruz
            String jsonResponse = objectMapper.writeValueAsString(response);
            channel.basicPublish("", queueName, null, jsonResponse.getBytes("UTF-8"));
            
            channel.close();
        } catch (Exception e) {
            log.error("Islem sirasinda hata olustu: ", e);
            logger.error("Yanit yayinlanirken hata olustu: {}", e.getMessage());
        }
    }
}
