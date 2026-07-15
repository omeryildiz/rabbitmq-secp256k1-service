package com.sgx.signature;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.sgx.signature.crypto.InMemorySigningKeyProvider;
import com.sgx.signature.crypto.Secp256k1Verifier;
import com.sgx.signature.model.SignRequest;
import com.sgx.signature.model.VerifyRequest;
import com.sgx.signature.rabbit.RabbitMqConnectionFactory;
import com.sgx.signature.rabbit.SignRequestConsumer;
import com.sgx.signature.rabbit.VerifyRequestConsumer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RabbitIntegrationTest {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final String KEY_ID = "integration-key";
    private static final String PAYLOAD = Base64.getEncoder()
            .encodeToString("RabbitMQ integration".getBytes(StandardCharsets.UTF_8));
    private static InMemorySigningKeyProvider keyProvider;

    @BeforeAll
    static void startServices() throws Exception {
        purgeQueues("sign.request", "sign.response", "verify.request", "verify.response");
        keyProvider = new InMemorySigningKeyProvider(KEY_ID);
        SignRequestConsumer.start(keyProvider);
        VerifyRequestConsumer.start();
    }

    @AfterAll
    static void closeConnection() throws Exception {
        Connection connection = RabbitMqConnectionFactory.getConnection();
        if (connection.isOpen()) {
            connection.close();
        }
    }

    @Test
    void connectsToRabbitMq() throws Exception {
        Connection connection = RabbitMqConnectionFactory.getConnection();
        assertNotNull(connection);
        assertTrue(connection.isOpen());
    }

    @Test
    void signsThroughRabbitMqAndReturnsVerifiableSignature() throws Exception {
        SignRequest request = new SignRequest();
        request.setRequestId(UUID.randomUUID().toString());
        request.setKeyId(KEY_ID);
        request.setPayload(PAYLOAD);

        JsonNode response = requestResponse("sign.request", "sign.response", request, request.getRequestId());

        assertEquals("OK", response.path("status").asText(), response.toString());
        assertTrue(Secp256k1Verifier.verifySignature(
                PAYLOAD,
                response.path("signature").asText(),
                response.path("publicKey").asText()));
    }

    @Test
    void verifiesValidSignatureThroughRabbitMq() throws Exception {
        String signature = keyProvider.sign(KEY_ID, PAYLOAD);
        VerifyRequest request = new VerifyRequest();
        request.setRequestId(UUID.randomUUID().toString());
        request.setPayload(PAYLOAD);
        request.setSignature(signature);
        request.setPublicKey(keyProvider.publicKeyBase64(KEY_ID));

        JsonNode response = requestResponse("verify.request", "verify.response", request, request.getRequestId());

        assertEquals("OK", response.path("status").asText(), response.toString());
        assertTrue(response.path("valid").asBoolean(), response.toString());
    }

    private static void purgeQueues(String... queues) throws Exception {
        Connection connection = RabbitMqConnectionFactory.getConnection();
        try (Channel channel = connection.createChannel()) {
            for (String queue : queues) {
                channel.queueDeclare(queue, false, false, false, null);
                channel.queuePurge(queue);
            }
        }
    }

    private static JsonNode requestResponse(
            String requestQueue, String responseQueue, Object request, String requestId) throws Exception {
        Connection connection = RabbitMqConnectionFactory.getConnection();
        try (Channel channel = connection.createChannel()) {
            channel.queueDeclare(requestQueue, false, false, false, null);
            channel.queueDeclare(responseQueue, false, false, false, null);
            channel.queuePurge(responseQueue);

            CompletableFuture<JsonNode> responseFuture = new CompletableFuture<>();
            String consumerTag = channel.basicConsume(responseQueue, true, (tag, delivery) -> {
                JsonNode response = OBJECT_MAPPER.readTree(delivery.getBody());
                if (requestId.equals(response.path("requestId").asText())) {
                    responseFuture.complete(response);
                }
            }, tag -> { });

            try {
                channel.basicPublish("", requestQueue, null, OBJECT_MAPPER.writeValueAsBytes(request));
                return responseFuture.get(10, TimeUnit.SECONDS);
            } finally {
                channel.basicCancel(consumerTag);
            }
        }
    }
}
