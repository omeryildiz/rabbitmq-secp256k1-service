package com.sgx.signature.crypto;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.util.io.pem.PemObject;
import org.bouncycastle.util.io.pem.PemWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileWriter;
import java.io.IOException;
import java.security.*;
import java.security.spec.ECGenParameterSpec;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

public class Secp256k1KeyManager {
    private static final Logger log = LoggerFactory.getLogger(java.lang.invoke.MethodHandles.lookup().lookupClass());
    private static final Logger logger = LoggerFactory.getLogger(Secp256k1KeyManager.class);

    static {
        // Bouncy Castle provider entegrasyonu
        Security.addProvider(new BouncyCastleProvider());
    }

    public static Map<String, String> generateKeyPair(String keyId) throws Exception {
        logger.info("Key pair uretimi basladi. Key ID: {}", keyId);

        // secp256k1 egrisi kullanilarak KeyPairGenerator olusturulmasi
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("ECDSA", "BC");
        ECGenParameterSpec ecSpec = new ECGenParameterSpec("secp256k1");
        keyPairGenerator.initialize(ecSpec, new SecureRandom());
        
        KeyPair keyPair = keyPairGenerator.generateKeyPair();
        
        String privateKeyFile = "keys/" + keyId + "-private.pem";
        String publicKeyFile = "keys/" + keyId + "-public.pem";

        // Anahtarlari dosyaya PEM formatinda yazma
        writePemFile(keyPair.getPrivate(), "PRIVATE KEY", privateKeyFile);
        writePemFile(keyPair.getPublic(), "PUBLIC KEY", publicKeyFile);

        // Public key'in Uncompressed veya X.509 formatinda alinmasi (Hex veya Base64 olabilir)
        String pubKeyBase64 = Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded());

        // JSON ciktisi icin sonuclari Map'e koyuyoruz
        Map<String, String> result = new HashMap<>();
        result.put("keyId", keyId);
        result.put("curve", "secp256k1");
        result.put("publicKey", pubKeyBase64);
        result.put("privateKeyFile", privateKeyFile);
        result.put("publicKeyFile", publicKeyFile);

        logger.info("Key uretimi tamamlandi ve dosyalara kaydedildi.");
        return result;
    }

    private static void writePemFile(Key key, String description, String filename) throws IOException {
        try (PemWriter pemWriter = new PemWriter(new FileWriter(filename))) {
            pemWriter.writeObject(new PemObject(description, key.getEncoded()));
        }
    }
}
