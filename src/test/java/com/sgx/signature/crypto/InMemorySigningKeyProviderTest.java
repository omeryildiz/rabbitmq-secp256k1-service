package com.sgx.signature.crypto;

import org.junit.jupiter.api.Test;

import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InMemorySigningKeyProviderTest {
    @Test
    void signsWithoutWritingPrivateKeyToDisk() throws Exception {
        InMemorySigningKeyProvider provider = new InMemorySigningKeyProvider("enclave-key");
        String payload = Base64.getEncoder().encodeToString("test".getBytes());

        String signature = provider.sign("enclave-key", payload);

        assertTrue(Secp256k1Verifier.verifySignature(
                payload, signature, provider.publicKeyBase64("enclave-key")));
    }

    @Test
    void rejectsAnotherKeyId() throws Exception {
        InMemorySigningKeyProvider provider = new InMemorySigningKeyProvider("enclave-key");
        String payload = Base64.getEncoder().encodeToString("test".getBytes());

        assertThrows(IllegalArgumentException.class, () -> provider.sign("other-key", payload));
    }

    @Test
    void rejectsPathTraversalInFileMode() {
        FileSigningKeyProvider provider = new FileSigningKeyProvider(java.nio.file.Path.of("keys"));
        assertThrows(IllegalArgumentException.class, () -> provider.sign("../secret", "dGVzdA=="));
    }
}
