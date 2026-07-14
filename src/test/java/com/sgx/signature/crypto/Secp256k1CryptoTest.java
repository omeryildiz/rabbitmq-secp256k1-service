package com.sgx.signature.crypto;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.util.Map;
import java.util.Base64;

public class Secp256k1CryptoTest {

    @Test
    public void testKeyGeneration() throws Exception {
        Map<String, String> keys = Secp256k1KeyManager.generateKeyPair("test-key-001");
        assertNotNull(keys, "Key map null olmamalıdır");
        assertNotNull(keys.get("publicKey"), "Public key üretilebilmelidir");
        assertNotNull(keys.get("publicKeyFile"), "Public key dosyası yolu dönmelidir");
    }

    @Test
    public void testSignAndVerifyHappyPath() throws Exception {
        String payloadBase64 = "SGVsbG8gU0dY"; 
        Map<String, String> keys = Secp256k1KeyManager.generateKeyPair("test-key-002");
        
        String privateKeyPath = "keys/test-key-002-private.pem"; 
        String pubKeyBase64 = keys.get("publicKey");
        
        String signatureBase64 = Secp256k1Signer.signPayload(payloadBase64, privateKeyPath);
        assertNotNull(signatureBase64, "İmza başarıyla oluşturulmalıdır");
        
        boolean isValid = Secp256k1Verifier.verifySignature(payloadBase64, signatureBase64, pubKeyBase64);
        assertTrue(isValid, "Doğru veri ve imza ile doğrulama BAŞARILI (true) olmalıdır");
    }

    @Test
    public void testVerifyWithWrongPayload() throws Exception {
        String originalPayloadBase64 = "SGVsbG8gU0dY";
        String wrongPayloadBase64 = "V1JPTkdfUEFZTE9BRA=="; 
        Map<String, String> keys = Secp256k1KeyManager.generateKeyPair("test-key-003");
        
        String privateKeyPath = "keys/test-key-003-private.pem"; 
        String pubKeyBase64 = keys.get("publicKey");
        
        String signatureBase64 = Secp256k1Signer.signPayload(originalPayloadBase64, privateKeyPath);
        
        boolean isValid = Secp256k1Verifier.verifySignature(wrongPayloadBase64, signatureBase64, pubKeyBase64);
        assertFalse(isValid, "Yanlış payload ile doğrulama BAŞARISIZ (false) olmalıdır");
    }

    @Test
    public void testVerifyWithWrongSignature() throws Exception {
        String payloadBase64 = "SGVsbG8gU0dY";
        Map<String, String> keys = Secp256k1KeyManager.generateKeyPair("test-key-004");
        
        String privateKeyPath = "keys/test-key-004-private.pem"; 
        String pubKeyBase64 = keys.get("publicKey");
        
        String signatureBase64 = Secp256k1Signer.signPayload(payloadBase64, privateKeyPath);
        
        // ÇÖZÜM BURADA: Base64 formatını kırmadan, imzanın kendisini içeriden matematiksel olarak bozuyoruz
        byte[] sigBytes = Base64.getDecoder().decode(signatureBase64);
        sigBytes[sigBytes.length / 2] ^= 1; // İmzanın ortasındaki bir baytı tersine çeviriyoruz
        String wrongSignatureBase64 = Base64.getEncoder().encodeToString(sigBytes);
        
        boolean isValid = Secp256k1Verifier.verifySignature(payloadBase64, wrongSignatureBase64, pubKeyBase64);
        assertFalse(isValid, "Bozulmuş imza ile doğrulama BAŞARISIZ (false) olmalıdır");
    }
}
