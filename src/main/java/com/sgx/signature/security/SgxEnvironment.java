package com.sgx.signature.security;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/** Fails closed before key generation when the SGX production profile is required. */
public final class SgxEnvironment {
    private static final Path ATTESTATION_TYPE = Path.of("/dev/attestation/attestation_type");
    private static final Path QUOTE = Path.of("/dev/attestation/quote");
    private static final int QUOTE_VERSION_3 = 3;
    private static final int ATTRIBUTES_FLAGS_OFFSET = 96;
    private static final long SGX_FLAGS_DEBUG = 0x2L;

    private SgxEnvironment() {
    }

    public static void requireDcapAttestation() {
        try {
            String attestationType = Files.readString(ATTESTATION_TYPE).trim();
            if (!"dcap".equalsIgnoreCase(attestationType)) {
                throw new IllegalStateException("DCAP SGX bekleniyordu, attestation_type=" + attestationType);
            }
            byte[] quote = Files.readAllBytes(QUOTE);
            if (isDebugQuote(quote)) {
                throw new IllegalStateException("Debug enclave içinde imzalama anahtarı üretmek reddedildi");
            }
        } catch (Exception e) {
            throw new IllegalStateException(
                    "Üretim SGX denetimi başarısız; bellek-içi imzalama anahtarı üretilmedi", e);
        }
    }

    static boolean isDebugQuote(byte[] quote) {
        if (quote.length < ATTRIBUTES_FLAGS_OFFSET + Long.BYTES) {
            throw new IllegalArgumentException("Geçersiz/kısa SGX quote");
        }
        ByteBuffer buffer = ByteBuffer.wrap(quote).order(ByteOrder.LITTLE_ENDIAN);
        int version = Short.toUnsignedInt(buffer.getShort(0));
        if (version != QUOTE_VERSION_3) {
            throw new IllegalArgumentException("Desteklenmeyen SGX quote sürümü: " + version);
        }
        long attributesFlags = buffer.getLong(ATTRIBUTES_FLAGS_OFFSET);
        return (attributesFlags & SGX_FLAGS_DEBUG) != 0;
    }
}
