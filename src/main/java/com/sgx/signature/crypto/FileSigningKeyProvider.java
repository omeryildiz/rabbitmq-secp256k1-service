package com.sgx.signature.crypto;

import java.nio.file.Files;
import java.nio.file.Path;

public final class FileSigningKeyProvider implements SigningKeyProvider {
    private final Path keyDirectory;

    public FileSigningKeyProvider(Path keyDirectory) {
        this.keyDirectory = keyDirectory;
    }

    @Override
    public String sign(String keyId, String payloadBase64) throws Exception {
        return Secp256k1Signer.signPayload(payloadBase64, privateKeyPath(keyId).toString());
    }

    @Override
    public String publicKeyBase64(String keyId) throws Exception {
        String pem = Files.readString(publicKeyPath(keyId));
        return pem.replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replaceAll("\\s", "");
    }

    @Override
    public String description() {
        return "file:" + keyDirectory;
    }

    private Path privateKeyPath(String keyId) {
        Secp256k1KeyManager.validateKeyId(keyId);
        Path path = keyDirectory.resolve(keyId + "-private.pem");
        if (!Files.isRegularFile(path)) {
            throw new IllegalArgumentException("Key not found: " + keyId);
        }
        return path;
    }

    private Path publicKeyPath(String keyId) {
        Secp256k1KeyManager.validateKeyId(keyId);
        Path path = keyDirectory.resolve(keyId + "-public.pem");
        if (!Files.isRegularFile(path)) {
            throw new IllegalArgumentException("Public key not found: " + keyId);
        }
        return path;
    }
}
