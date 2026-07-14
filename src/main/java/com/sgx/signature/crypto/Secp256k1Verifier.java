package com.sgx.signature.crypto;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

public class Secp256k1Verifier {
    private static final Logger log = LoggerFactory.getLogger(java.lang.invoke.MethodHandles.lookup().lookupClass());

    public static boolean verifySignature(String payloadBase64, String signatureBase64, String publicKeyBase64) throws Exception {
        // 1. Base64 formatındaki Public Key'i X.509 formatında Java nesnesine çevirme
        byte[] pubKeyBytes = Base64.getDecoder().decode(publicKeyBase64);
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(pubKeyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance("ECDSA", "BC");
        PublicKey publicKey = keyFactory.generatePublic(keySpec);

        // 2. Base64 payload ve imzayı decode etme
        byte[] payloadBytes = Base64.getDecoder().decode(payloadBase64);
        byte[] signatureBytes = Base64.getDecoder().decode(signatureBase64);

        // 3. İmzayı doğrulama
        Signature signature = Signature.getInstance("SHA256withECDSA", "BC");
        signature.initVerify(publicKey);
        signature.update(payloadBytes);
        
        return signature.verify(signatureBytes);
    }
}
