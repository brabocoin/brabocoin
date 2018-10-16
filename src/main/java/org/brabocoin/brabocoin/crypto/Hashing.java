package org.brabocoin.brabocoin.crypto;

import com.google.protobuf.ByteString;
import org.bouncycastle.jcajce.provider.digest.SHA256;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.brabocoin.brabocoin.model.Hash;
import org.jetbrains.annotations.NotNull;

import java.security.MessageDigest;
import java.security.Security;

/**
 * Hashing functions.
 */
public final class Hashing {

    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    private Hashing() {
    }

    /**
     * Compute the SHA-256 hash of a message.
     *
     * @param message
     *         The message to be hashed.
     * @return The hashed message.
     */
    public static Hash digestSHA256(@NotNull ByteString message) {
        return digest(new SHA256.Digest(), message);
    }

    /**
     * Compute the SHA-256 hash of a message.
     *
     * @param message
     *         The message to be hashed.
     * @return The hashed message.
     */
    public static Hash digestSHA256(@NotNull Hash message) {
        return digestSHA256(message.getValue());
    }

    private static @NotNull Hash digest(@NotNull MessageDigest messageDigest,
                                        @NotNull ByteString message) {
        return new Hash(ByteString.copyFrom(messageDigest.digest(message.toByteArray())));
    }

}
