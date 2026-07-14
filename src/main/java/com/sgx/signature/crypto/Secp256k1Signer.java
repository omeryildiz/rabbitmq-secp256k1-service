package com.sgx.signature.crypto;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.bouncycastle.util.io.pem.PemObject;
import org.bouncycastle.util.io.pem.PemReader;

import java.io.FileReader;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;

public class Secp256k1Signer {
    private static final Logger log = LoggerFactory.getLogger(java.lang.invoke.MethodHandles.lookup().lookupClass());
    
    public static String signPayload(String payloadBase64, String privateKeyPath) throws Exception {
        // 1. PEM dosyasından Private Key'i okuma
        PrivateKey privateKey;
        try (PemReader pemReader = new PemReader(new FileReader(privateKeyPath))) {
            PemObject pemObject = pemReader.readPemObject();
            if (pemObject == null) {
                throw new IllegalArgumentException("Private key dosyasi okunamadi: " + privateKeyPath);
            }
            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(pemObject.getContent());
            KeyFactory keyFactory = KeyFactory.getInstance("ECDSA", "BC");
            privateKey = keyFactory.generatePrivate(keySpec);
        }

        // 2. Base64 formattaki payload'u byte dizisine çevirme
        byte[] payloadBytes = Base64.getDecoder().decode(payloadBase64);

        // 3. SHA256withECDSA kullanarak imzalama (Bouncy Castle varsayılan olarak DER formatında imza üretir)
        Signature signature = Signature.getInstance("SHA256withECDSA", "BC");
        signature.initSign(privateKey);
        signature.update(payloadBytes);
        byte[] signatureBytes = signature.sign();

        // 4. İmzayı JSON'da taşınabilmesi için Base64'e çevirip dönme
        return Base64.getEncoder().encodeToString(signatureBytes);
    }
}
