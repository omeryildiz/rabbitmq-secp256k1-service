package com.sgx.signature;

import com.sgx.signature.crypto.Secp256k1KeyManager;
import com.sgx.signature.rabbit.SignRequestConsumer;
import com.sgx.signature.rabbit.VerifyRequestConsumer;
import com.sgx.signature.benchmark.BenchmarkClient;
import org.bouncycastle.jce.provider.BouncyCastleProvider; // EKLENDİ

import java.security.Security; // EKLENDİ
import java.util.Map;
import java.util.HashMap;
import java.util.Arrays;
import java.util.List;

public class Main {
    public static void main(String[] args) {
        // UYGULAMANIN EN BAŞINDA BOUNCY CASTLE SİSTEME TANITILIYOR
        Security.addProvider(new BouncyCastleProvider());

        Map<String, String> parsedArgs = parseArgs(args);
        String mode = parsedArgs.getOrDefault("mode", "");

        try {
            if ("keygen".equalsIgnoreCase(mode)) {
                String keyId = parsedArgs.getOrDefault("key-id", "default-key");
                Map<String, String> result = Secp256k1KeyManager.generateKeyPair(keyId);
                
                System.out.println("{");
                System.out.println("  \"keyId\": \"" + result.get("keyId") + "\",");
                System.out.println("  \"curve\": \"" + result.get("curve") + "\",");
                System.out.println("  \"publicKey\": \"" + result.get("publicKey") + "\",");
                System.out.println("  \"privateKeyFile\": \"" + result.get("privateKeyFile") + "\",");
                System.out.println("  \"publicKeyFile\": \"" + result.get("publicKeyFile") + "\"");
                System.out.println("}");
            } else if ("signer".equalsIgnoreCase(mode)) {
                System.out.println("Signer modu baslatiliyor...");
                SignRequestConsumer.start();
            } else if ("verifier".equalsIgnoreCase(mode)) {
                System.out.println("Verifier modu baslatiliyor...");
                VerifyRequestConsumer.start();
            } else if ("benchmark-client".equalsIgnoreCase(mode)) {
                String operation = parsedArgs.getOrDefault("operation", "sign");
                int messageCount = Integer.parseInt(parsedArgs.getOrDefault("message-count", "10000"));
                int payloadSize = Integer.parseInt(parsedArgs.getOrDefault("payload-size", "32"));
                
                BenchmarkClient.start(operation, messageCount, payloadSize);
            } else {
                System.out.println("Lutfen gecerli bir mod girin (keygen, signer, verifier, benchmark-client)");
            }
        } catch (Exception e) {
            System.err.println("Islem sirasinda hata olustu: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static Map<String, String> parseArgs(String[] args) {
        Map<String, String> map = new HashMap<>();
        for (String arg : args) {
            if (arg.startsWith("--")) {
                String[] parts = arg.substring(2).split("=", 2);
                if (parts.length == 2) {
                    List<String> partsList = Arrays.asList(parts);
                    map.put(partsList.get(0), partsList.get(1));
                }
            }
        }
        return map;
    }
}
