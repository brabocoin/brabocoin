package org.brabocoin.brabocoin.util;

import com.google.protobuf.ByteString;
import org.brabocoin.brabocoin.crypto.Hashing;
import org.jetbrains.annotations.NotNull;

import java.math.BigInteger;

/**
 * Base58 checksum encoding.
 */
public final class Base58Check {

    /**
     * Equivalent with the Base64 alphabet, without 0 (zero), O (capital o), I (capital i) and l
     * (lowercase l) and non-alphanumeric characters (+, /, etc.)
     */
    private static final String ALPHABET =
        "123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz";

    /**
     * The base of the encoding, based on the alphabet length.
     */
    private static final BigInteger BASE = BigInteger.valueOf(ALPHABET.length());

    /**
     * The length of the checksum at the end of the encoded string.
     */
    private static final int CHECKSUM_SIZE = 4;

    private Base58Check() {

    }

    /**
     * Encode data into {@code Base58check}.
     *
     * @param data
     *     The data to encode.
     * @return The encoded string.
     */
    public static @NotNull String encode(@NotNull ByteString data) {
        ByteString checkedData = data.concat(getChecksum(data));

        return toBase58(checkedData);
    }

    private static @NotNull String toBase58(@NotNull ByteString data) {
        BigInteger value = new BigInteger(1, data.toByteArray());

        StringBuilder result = new StringBuilder();

        while (value.compareTo(BigInteger.ZERO) > 0) {
            BigInteger[] divRem = value.divideAndRemainder(BASE);

            value = divRem[0];
            result.append(ALPHABET.charAt(divRem[1].intValue()));
        }

        // Prepend leading zeros
        int i = 0;
        while (i < data.size() && data.byteAt(i) == 0x00) {
            result.append(ALPHABET.charAt(0));
            i++;
        }

        return result.reverse().toString();
    }

    private static ByteString getChecksum(@NotNull ByteString data) {
        return Hashing.digestSHA256(Hashing.digestSHA256(data)).getValue()
            .substring(0, CHECKSUM_SIZE);
    }

    /**
     * Decode a {@code Base58check} encoded string into raw bytes.
     *
     * @param data
     *     The data to decode.
     * @return The decoded raw bytes.
     * @throws IllegalArgumentException
     *     When the input data is not a valid {@code Base58check} encoded string.
     */
    public static @NotNull ByteString decode(@NotNull String data) throws IllegalArgumentException {
        ByteString decoded = fromBase58(data);

        if (decoded.size() < CHECKSUM_SIZE) {
            throw new IllegalArgumentException("Input data is too short.");
        }

        ByteString checksum = decoded.substring(decoded.size() - CHECKSUM_SIZE, decoded.size());
        ByteString value = decoded.substring(0, decoded.size() - CHECKSUM_SIZE);

        if (!getChecksum(value).equals(checksum)) {
            throw new IllegalArgumentException("Checksum does not match.");
        }

        return value;
    }

    private static @NotNull ByteString fromBase58(@NotNull String data) {
        BigInteger result = BigInteger.ZERO;

        for (int i = data.length() - 1; i >= 0; i--) {
            int decoded = ALPHABET.indexOf(data.charAt(i));

            if (decoded == -1) {
                throw new IllegalArgumentException("Provided data is not encoded in Base58.");
            }

            result = result.add(BASE.pow(data.length() - i - 1)
                .multiply(BigInteger.valueOf(decoded)));
        }

        // Prepend leading ones as zeroes
        int i = 0;
        while (i < data.length() && data.charAt(i) == ALPHABET.charAt(0)) {
            i++;
        }

        return ByteString.copyFrom(new byte[i]).concat(ByteUtil.toUnsigned(result));
    }

}
