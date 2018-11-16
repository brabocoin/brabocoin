package org.brabocoin.brabocoin.util;

import org.jetbrains.annotations.NotNull;

import java.math.BigInteger;

/**
 * Utility methods for {@link BigInteger}.
 */
public final class BigIntegerUtil {

    private BigIntegerUtil() {

    }

    /**
     * Checks whether the value is within the range defined by {@code min} and {@code max}.
     *
     * @param value The value to check if it is in the range.
     * @param min   The minimum value.
     * @param max   The maximum value.
     * @return Whether {@code min <= value <= max}.
     */
    public static boolean isInRange(@NotNull BigInteger value, @NotNull BigInteger min,
                                    @NotNull BigInteger max) {
        return value.compareTo(min) >= 0 && value.compareTo(max) <= 0;
    }

    /**
     * Checks whether the value is within the range defined by {@code min} and {@code max}, with exclusive bounds.
     *
     * @param value The value to check if it is in the range.
     * @param min   The minimum value.
     * @param max   The maximum value.
     * @return Whether {@code min < value < max}.
     */
    public static boolean isInRangeExclusive(@NotNull BigInteger value, @NotNull BigInteger min,
                                             @NotNull BigInteger max) {
        return value.compareTo(min) > 0 && value.compareTo(max) < 0;
    }

    /**
     * Get the max BigInteger for a given number of bytes.
     *
     * @param byteCount Number of bytes
     * @return Max BigInteger for the given number of bytes
     */
    public static BigInteger getMaxBigInteger(int byteCount) {
        byte[] maxByteArray = new byte[byteCount];
        for (int i = 0; i < byteCount; i++) {
            maxByteArray[i] = Byte.MAX_VALUE;
        }
        return new BigInteger(maxByteArray);
    }

}
