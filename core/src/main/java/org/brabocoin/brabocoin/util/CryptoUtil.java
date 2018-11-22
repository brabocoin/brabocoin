package org.brabocoin.brabocoin.util;

import java.security.SecureRandom;

public class CryptoUtil {
    private static final SecureRandom random = new SecureRandom();

    /**
     * Generate random bytes.
     *
     * @param length Length of byte sequence
     * @return Byte array of random bytes
     */
    public static byte[] generateBytes(int length) {
        byte[] salt = new byte[length];
        random.nextBytes(salt);
        return salt;
    }
}
