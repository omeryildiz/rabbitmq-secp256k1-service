package com.sgx.signature;

import com.sgx.signature.crypto.Secp256k1KeyManager;
import com.sgx.signature.crypto.Secp256k1Signer;
import com.sgx.signature.crypto.Secp256k1Verifier;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.security.Security;
import java.util.Map;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

public class CryptoTest {

    @BeforeAll
    public static void setup() {
        Security.addProvider(new BouncyCastleProvider());
    }

    @Test
    public void testCryptoOperations() {
        try {
            // 1. secp256k1 key generation testi
            Map<String, String> keys = Secp256k1KeyManager.generateKeyPair("test-unit-key");
            assertNotNull(keys.get("privateKeyFile"));
            assertNotNull(keys.get("publicKey"));

            String payload = "SGVsbG8gU0dY"; // Base64 örnek
            String privateKeyPath = keys.get("privateKeyFile");
            
            // Public key'i temizle
            String pubKeyStr = new String(Files.readAllBytes(Paths.get(keys.get("publicKeyFile"))))
                    .replace("-----BEGIN PUBLIC KEY-----", "")
                    .replace("-----END PUBLIC KEY-----", "")
                    .replaceAll("\\s", "");

            // 2. Sign/verify happy path testi
            String signature = Secp256k1Signer.signPayload(payload, privateKeyPath);
            assertNotNull(signature);
            boolean isValid = Secp256k1Verifier.verifySignature(payload, signature, pubKeyStr);
            assertTrue(isValid, "Happy path dogrulamasi basarisiz!");

            // 3. Yanlış payload ile verify false dönme testi
            // Geçerli bir Base64 dizgesi ile sahte payload oluşturuyoruz
            String fakeBase64Payload = "V3JvbmdQYXlsb2Fk"; 
            boolean isInvalidPayload = false;
            try {
                isInvalidPayload = Secp256k1Verifier.verifySignature(fakeBase64Payload, signature, pubKeyStr);
            } catch (Exception e) {
                // Hata fırlatılırsa da doğrulanamamış (false) demektir
            }
            assertFalse(isInvalidPayload, "Yanlis payload true dondu!");

            // 4. Yanlış signature ile verify false dönme testi
            // İmzanın son karakterini değiştirerek bozuyoruz
            String badSignature = signature.substring(0, signature.length() - 5) + "AAAAA";
            boolean isInvalidSignature = false;
            try {
                isInvalidSignature = Secp256k1Verifier.verifySignature(payload, badSignature, pubKeyStr);
            } catch (Exception e) {
                // Bouncy Castle bozuk imzada exception fırlatabilir, bu da false demektir.
            }
            assertFalse(isInvalidSignature, "Yanlis imza true dondu!");

        } catch (Exception e) {
            fail("Test sirasinda beklenmeyen hata: " + e.getMessage());
        }
    }
}
