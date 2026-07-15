package com.sgx.signature.crypto;

public interface SigningKeyProvider {
    String sign(String keyId, String payloadBase64) throws Exception;

    String publicKeyBase64(String keyId) throws Exception;

    String description();
}
