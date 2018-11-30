package org.brabocoin.brabocoin.util;

import com.google.protobuf.ByteString;
import org.bouncycastle.util.encoders.Hex;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.params.provider.Arguments.arguments;

/**
 * Test {@link Base58Check}.
 */
class Base58CheckTest {

    @Test
    void decodeEmpty() {
        assertThrows(IllegalArgumentException.class, () -> Base58Check.decode(""));
    }

    @Test
    void decodeNotValidChecksum() {
        assertThrows(IllegalArgumentException.class, () -> Base58Check.decode("1111111111"));
    }

    @Test
    void decodeTooShort() {
        assertThrows(IllegalArgumentException.class, () -> Base58Check.decode("111"));
    }


    @ParameterizedTest(name = "[{index}] Address {1}")
    @MethodSource("base58CheckProvider")
    void encodeAndDecode(ByteString data, String encoded) {
        assertEquals(encoded, Base58Check.encode(data));
        assertEquals(data, Base58Check.decode(encoded));
    }

    static Stream<Arguments> base58CheckProvider() {
        return Stream.of(
            arguments(
                ByteString.copyFrom(Hex.decode("00086eaa677895f92d4a6c5ef740c168932b5e3f44")),
                "1mayif3H2JDC62S4N3rLNtBNRAiUUP99k"
            ),
            arguments(
                ByteString.copyFrom(Hex.decode("001072827c4b02af9fa78380f7befa06ae28a5635d")),
                "12VxzLv3hmmHSQGWbpo8BAm8EhMgkZMctr"
            ),
            arguments(
                ByteString.copyFrom(Hex.decode("0536e88308aeeec46d7f92d0a823610ec4185b1a94")),
                "36hLx7HjsVLK49RH1A5ExAnd8R4ML8yEmT"
            )
        );
    }
}
