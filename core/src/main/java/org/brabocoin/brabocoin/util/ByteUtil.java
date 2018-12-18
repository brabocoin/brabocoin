package org.brabocoin.brabocoin.util;

import com.google.common.primitives.Ints;
import com.google.common.primitives.Longs;
import com.google.protobuf.ByteString;
import org.jetbrains.annotations.NotNull;

import javax.xml.bind.DatatypeConverter;
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
     *     The value to convert.
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
     *     The value to convert.
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
     *     The value to convert.
     * @return The integer representation of the value.
     */
    public static int toInt(@NotNull ByteString value) {
        return ByteBuffer.wrap(value.toByteArray()).getInt();
    }

    /**
     * Convert a ByteString to a hexadecimal string representation of the bytes.
     *
     * @param byteString
     *     The value to convert.
     * @return The hexadecimal representation of the bytes in a string.
     */
    public static String toHexString(ByteString byteString) {
        return toHexString(byteString, byteString.size());
    }

    /**
     * Convert a ByteString to a hexadecimal string representation of the bytes.
     *
     * @param byteString
     *     The value to convert.
     * @param size
     *     The size (in bytes) the string is printed. If the size of the {@code byteString} is
     *     smaller than the given size, the string is zero-padded.
     * @return The hexadecimal representation of the bytes in a string.
     */
    public static String toHexString(ByteString byteString, int size) {
        if (byteString == null) {
            return "null";
        }

        int lengthDifference = size - byteString.size();

        if (lengthDifference > 0) {
            byteString = ByteString.copyFrom(new byte[lengthDifference]).concat(byteString);
        }

        return DatatypeConverter.printHexBinary(byteString.toByteArray());
    }

    /**
     * Parses a string in hexadecimal representation to ByteString.
     *
     * @param hexString
     *     The string to format
     * @return Bytestring object
     */
    public static ByteString fromHexString(String hexString) {
        if (hexString == null) {
            return ByteString.EMPTY;
        }

        return ByteString.copyFrom(DatatypeConverter.parseHexBinary(hexString));
    }
}
