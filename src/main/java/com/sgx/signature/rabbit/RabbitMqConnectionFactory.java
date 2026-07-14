package com.sgx.signature.rabbit;

import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RabbitMqConnectionFactory {
    private static final Logger logger = LoggerFactory.getLogger(RabbitMqConnectionFactory.class);
    private static Connection connection;

    public static Connection getConnection() throws Exception {
        if (connection == null || !connection.isOpen()) {
            ConnectionFactory factory = new ConnectionFactory();
            // Not: İdealde AppConfig üzerinden properties'den okunmalı, şimdilik gereksinimlerdeki varsayılanları kullanıyoruz.
            factory.setHost("localhost");
            factory.setPort(5672);
            factory.setUsername("sgxuser");
            factory.setPassword("sgxpass");
            
            // Riskler bölümünde istenen otomatik yeniden bağlanma yeteneği
            factory.setAutomaticRecoveryEnabled(true);
            factory.setNetworkRecoveryInterval(5000);

            connection = factory.newConnection();
            logger.info("RabbitMQ baglantisi basariyla acildi.");
        }
        return connection;
    }
}
