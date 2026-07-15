package com.sgx.signature.security;

import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SgxEnvironmentTest {
    @Test
    void detectsDebugFlagInDcapQuoteV3() {
        byte[] quote = quoteWithFlags(0x2L);
        assertTrue(SgxEnvironment.isDebugQuote(quote));
    }

    @Test
    void acceptsProductionFlagInDcapQuoteV3() {
        byte[] quote = quoteWithFlags(0x4L);
        assertFalse(SgxEnvironment.isDebugQuote(quote));
    }

    @Test
    void rejectsUnknownOrTruncatedQuote() {
        assertThrows(IllegalArgumentException.class,
                () -> SgxEnvironment.isDebugQuote(new byte[10]));
        byte[] quote = quoteWithFlags(0x4L);
        quote[0] = 4;
        assertThrows(IllegalArgumentException.class,
                () -> SgxEnvironment.isDebugQuote(quote));
    }

    private static byte[] quoteWithFlags(long flags) {
        byte[] quote = new byte[104];
        ByteBuffer buffer = ByteBuffer.wrap(quote).order(ByteOrder.LITTLE_ENDIAN);
        buffer.putShort(0, (short) 3);
        buffer.putLong(96, flags);
        return quote;
    }
}
