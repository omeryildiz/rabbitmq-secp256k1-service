package com.sgx.signature.crypto;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.util.io.pem.PemObject;
import org.bouncycastle.util.io.pem.PemWriter;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.security.*;
import java.security.spec.ECGenParameterSpec;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

public class Secp256k1KeyManager {
    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(Secp256k1KeyManager.class);
    private static final String KEY_ID_PATTERN = "[A-Za-z0-9._-]{1,64}";

    static {
        // Bouncy Castle provider entegrasyonu
        Security.addProvider(new BouncyCastleProvider());
    }

    public static Map<String, String> generateKeyPair(String keyId) throws Exception {
        return generateKeyPair(keyId, Path.of("keys"));
    }

    public static Map<String, String> generateKeyPair(String keyId, Path keyDirectory) throws Exception {
        validateKeyId(keyId);
        logger.info("Key pair uretimi basladi. Key ID: {}", keyId);

        KeyPair keyPair = generateInMemoryKeyPair();
        Files.createDirectories(keyDirectory);
        Path privateKeyFile = keyDirectory.resolve(keyId + "-private.pem");
        Path publicKeyFile = keyDirectory.resolve(keyId + "-public.pem");

        // Anahtarlari dosyaya PEM formatinda yazma
        writePemFile(keyPair.getPrivate(), "PRIVATE KEY", privateKeyFile, true);
        writePemFile(keyPair.getPublic(), "PUBLIC KEY", publicKeyFile, false);

        // Public key'in Uncompressed veya X.509 formatinda alinmasi (Hex veya Base64 olabilir)
        String pubKeyBase64 = Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded());

        // JSON ciktisi icin sonuclari Map'e koyuyoruz
        Map<String, String> result = new HashMap<>();
        result.put("keyId", keyId);
        result.put("curve", "secp256k1");
        result.put("publicKey", pubKeyBase64);
        result.put("privateKeyFile", privateKeyFile.toString());
        result.put("publicKeyFile", publicKeyFile.toString());

        logger.info("Key uretimi tamamlandi ve dosyalara kaydedildi.");
        return result;
    }

    static KeyPair generateInMemoryKeyPair() throws GeneralSecurityException {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("ECDSA", "BC");
        generator.initialize(new ECGenParameterSpec("secp256k1"), new SecureRandom());
        return generator.generateKeyPair();
    }

    static void validateKeyId(String keyId) {
        if (keyId == null || !keyId.matches(KEY_ID_PATTERN)) {
            throw new IllegalArgumentException("key-id yalnızca harf, rakam, nokta, alt çizgi ve tire içerebilir (1-64 karakter)");
        }
    }

    private static void writePemFile(Key key, String description, Path filename, boolean privateKey) throws IOException {
        try (Writer writer = Files.newBufferedWriter(filename);
             PemWriter pemWriter = new PemWriter(writer)) {
            pemWriter.writeObject(new PemObject(description, key.getEncoded()));
        }
        if (privateKey && Files.getFileStore(filename).supportsFileAttributeView("posix")) {
            Files.setPosixFilePermissions(filename, PosixFilePermissions.fromString("rw-------"));
        }
    }
}
