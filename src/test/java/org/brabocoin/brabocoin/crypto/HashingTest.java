package org.brabocoin.brabocoin.crypto;

import com.google.protobuf.ByteString;
import org.brabocoin.brabocoin.model.Hash;
import org.junit.jupiter.api.Test;

import javax.xml.bind.DatatypeConverter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

/**
 * Test calculating hashes with the SHA-256 algorithm.
 * Hashes are calculated with https://passwordsgenerator.net/sha256-hash-generator/.
 *
 * @author David Dekker.
 */
class HashingTest {
    @Test
    void testHashing() {
        String input = "test";
        ByteString testExpected = ByteString.copyFrom(DatatypeConverter.parseHexBinary("9F86D081884C7D659A2FEAA0C55AD015A3BF4F1B2B0B822CD15D6C15B0F00A08"));
        ByteString testActual = Hashing.digestSHA256(ByteString.copyFromUtf8(input)).getValue();
        ByteString testActualWithHash = Hashing.digestSHA256(new Hash(ByteString.copyFromUtf8(input))).getValue();

        String input2 = "test2";
        ByteString test2Expected = ByteString.copyFrom(DatatypeConverter.parseHexBinary("60303AE22B998861BCE3B28F33EEC1BE758A213C86C93C076DBE9F558C11C752"));
        ByteString test2Actual = Hashing.digestSHA256(ByteString.copyFromUtf8(input2)).getValue();
        ByteString test2ActualWithHash = Hashing.digestSHA256(new Hash(ByteString.copyFromUtf8(input2))).getValue();

        String input3 = "This is a very long message which is about to be hashed by the SHA-256 algorithm which will produce a 256-bit hash.";
        ByteString longInputExpected = ByteString.copyFrom(DatatypeConverter.parseHexBinary("D0D74FBB1B1E345EC72F57CDFBBC5A4DED7A6FCFEAE7D8938111D9D470B307AC"));
        ByteString longInputActual = Hashing.digestSHA256(ByteString.copyFromUtf8(input3)).getValue();
        ByteString longInputActualWithHash = Hashing.digestSHA256(new Hash(ByteString.copyFromUtf8(input3))).getValue();

        assertEquals(testExpected, testActual);
        assertEquals(testExpected, testActualWithHash);

        assertEquals(test2Expected, test2Actual);
        assertEquals(test2Expected, test2ActualWithHash);

        assertEquals(longInputExpected, longInputActual);
        assertEquals(longInputExpected, longInputActualWithHash);

        assertNotEquals(testExpected, test2Actual);
        assertNotEquals(test2Expected, longInputActualWithHash);
    }
}
