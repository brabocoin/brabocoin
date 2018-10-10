package org.brabocoin.brabocoin.util;

import com.google.protobuf.ByteString;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Sten Wessel
 */
class ByteUtilTest {

    private static byte[] ZERO_INT = new byte[] {0x00, 0x00, 0x00, 0x00};
    private static byte[] MIN_INT = new byte[] {(byte)0x80, 0x00, 0x00, 0x00};
    private static byte[] MAX_INT = new byte[] {0x7f, (byte)0xff, (byte)0xff, (byte)0xff};
    private static byte[] ONE_INT = new byte[] {0x00, 0x00, 0x00, 0x01};
    private static byte[] NEGATIVE_ONE_INT = new byte[] {
            (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff
    };
    private static byte[] NINETEEN_EIGHTY_FOUR_INT = new byte[] {0x00, 0x00, 0x07, (byte)0xc0};

    private static byte[] ZERO_LONG = new byte[] {0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00};
    private static byte[] MIN_LONG = new byte[] {
            (byte)0x80, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00
    };
    private static byte[] MAX_LONG = new byte[] {
            0x7f, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff
    };
    private static byte[] ONE_LONG = new byte[] {0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x01};
    private static byte[] NEGATIVE_ONE_LONG = new byte[] {
            (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff,
            (byte)0xff
    };
    private static byte[] NINETEEN_EIGHTY_FOUR_LONG = new byte[] {
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x07, (byte)0xc0
    };

    @Test
    void testIntToByteString() {
        assertEquals(ByteString.copyFrom(ZERO_INT), ByteUtil.toByteString(0));
        assertEquals(ByteString.copyFrom(MAX_INT), ByteUtil.toByteString(Integer.MAX_VALUE));
        assertEquals(ByteString.copyFrom(MIN_INT), ByteUtil.toByteString(Integer.MIN_VALUE));
        assertEquals(ByteString.copyFrom(ONE_INT), ByteUtil.toByteString(1));
        assertEquals(ByteString.copyFrom(NEGATIVE_ONE_INT), ByteUtil.toByteString(-1));
        assertEquals(ByteString.copyFrom(NINETEEN_EIGHTY_FOUR_INT), ByteUtil.toByteString(1984));
    }

    @Test
    void testLongToByteString() {
        assertEquals(ByteString.copyFrom(ZERO_LONG), ByteUtil.toByteString(0L));
        assertEquals(ByteString.copyFrom(MAX_LONG), ByteUtil.toByteString(Long.MAX_VALUE));
        assertEquals(ByteString.copyFrom(MIN_LONG), ByteUtil.toByteString(Long.MIN_VALUE));
        assertEquals(ByteString.copyFrom(ONE_LONG), ByteUtil.toByteString(1L));
        assertEquals(ByteString.copyFrom(NEGATIVE_ONE_LONG), ByteUtil.toByteString(-1L));
        assertEquals(ByteString.copyFrom(NINETEEN_EIGHTY_FOUR_LONG), ByteUtil.toByteString(1984L));
    }

    @Test
    void testToInt() {
        assertEquals(ByteUtil.toInt(ZERO_INT), 0);
        assertEquals(ByteUtil.toInt(MAX_INT), Integer.MAX_VALUE);
        assertEquals(ByteUtil.toInt(MIN_INT), Integer.MIN_VALUE);
        assertEquals(ByteUtil.toInt(ONE_INT), 1);
        assertEquals(ByteUtil.toInt(NEGATIVE_ONE_INT), -1);
        assertEquals(ByteUtil.toInt(NINETEEN_EIGHTY_FOUR_INT), 1984);
    }
}
