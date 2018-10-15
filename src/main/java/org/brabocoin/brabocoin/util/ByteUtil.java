package org.brabocoin.brabocoin.util;

import com.google.common.primitives.Ints;
import com.google.common.primitives.Longs;
import com.google.protobuf.ByteString;
import org.jetbrains.annotations.NotNull;

import java.nio.ByteBuffer;

/**
 * Utility class for raw byte data structures.
 */
public final class ByteUtil {

    private ByteUtil() {
    }

    /**
     * Convert a long value to a byte string.
     * <p>
     * The resulting byte string will be 8 bytes long.
     *
     * @param value
     *         The value to convert.
     * @return The byte string representation of the value.
     */
    public static @NotNull ByteString toByteString(long value) {
        return ByteString.copyFrom(Longs.toByteArray(value));
    }

    /**
     * Convert an integer value to a byte string.
     * <p>
     * The resulting byte string will be 4 bytes long.
     *
     * @param value
     *         The value to convert.
     * @return The byte string representation of the value.
     */
    public static @NotNull ByteString toByteString(int value) {
        return ByteString.copyFrom(Ints.toByteArray(value));
    }

    /**
     * Convert a byte array to an integer value.
     * <p>
     * Note: only the first 4 bytes from the array will be read.
     * If the byte array contains more data, the following bytes are ignored.
     *
     * @param value
     *         The value to convert.
     * @return The integer representation of the value.
     */
    public static int toInt(@NotNull ByteString value) {
        return ByteBuffer.wrap(value.toByteArray()).getInt();
    }
}