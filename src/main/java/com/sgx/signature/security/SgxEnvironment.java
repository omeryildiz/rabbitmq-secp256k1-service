package com.sgx.signature.security;

import java.nio.file.Files;
import java.nio.file.Path;

/** Fails closed before key generation when the SGX production profile is required. */
public final class SgxEnvironment {
    private static final Path ATTESTATION_TYPE = Path.of("/dev/attestation/attestation_type");

    private SgxEnvironment() {
    }

    public static void requireDcapAttestation() {
        try {
            String attestationType = Files.readString(ATTESTATION_TYPE).trim();
            if (!"dcap".equalsIgnoreCase(attestationType)) {
                throw new IllegalStateException("DCAP SGX bekleniyordu, attestation_type=" + attestationType);
            }
        } catch (Exception e) {
            throw new IllegalStateException(
                    "Üretim SGX denetimi başarısız; bellek-içi imzalama anahtarı üretilmedi", e);
        }
    }
}
