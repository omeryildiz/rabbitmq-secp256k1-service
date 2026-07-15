package com.sgx.signature.crypto;

import java.security.KeyPair;
import java.util.Base64;

/** Holds one non-exportable-by-API signing key for the lifetime of this JVM. */
public final class InMemorySigningKeyProvider implements SigningKeyProvider {
    private final String keyId;
    private final KeyPair keyPair;
    private final String publicKeyBase64;

    public InMemorySigningKeyProvider(String keyId) throws Exception {
        Secp256k1KeyManager.validateKeyId(keyId);
        this.keyId = keyId;
        this.keyPair = Secp256k1KeyManager.generateInMemoryKeyPair();
        this.publicKeyBase64 = Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded());
    }

    @Override
    public String sign(String requestedKeyId, String payloadBase64) throws Exception {
        requireExpectedKey(requestedKeyId);
        return Secp256k1Signer.signPayload(payloadBase64, keyPair.getPrivate());
    }

    @Override
    public String publicKeyBase64(String requestedKeyId) {
        requireExpectedKey(requestedKeyId);
        return publicKeyBase64;
    }

    @Override
    public String description() {
        return "memory-only:" + keyId;
    }

    private void requireExpectedKey(String requestedKeyId) {
        if (!keyId.equals(requestedKeyId)) {
            throw new IllegalArgumentException("Bu signer yalnızca '" + keyId + "' key-id değerini kabul eder");
        }
    }
}
