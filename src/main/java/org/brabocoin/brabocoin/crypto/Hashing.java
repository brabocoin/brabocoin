package org.brabocoin.brabocoin.crypto;

import com.google.protobuf.ByteString;
import org.bouncycastle.jcajce.provider.digest.SHA256;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.brabocoin.brabocoin.model.Hash;
import org.jetbrains.annotations.NotNull;

import java.security.MessageDigest;
import java.security.Security;
import java.text.MessageFormat;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.brabocoin.brabocoin.util.ByteUtil.toHexString;

/**
 * Hashing functions.
 */
public final class Hashing {
    private static final Logger LOGGER = Logger.getLogger(Hashing.class.getName());
    private static SHA256.Digest SHA256Digest = new SHA256.Digest();

    static {
        LOGGER.fine("Hashing class initializing.");
        Security.addProvider(new BouncyCastleProvider());
    }

    private Hashing() {
    }

    /**
     * Compute the SHA-256 hash of a message.
     *
     * @param message The message to be hashed.
     * @return The hashed message.
     */
    public static Hash digestSHA256(@NotNull ByteString message) {
        LOGGER.fine("Digest SHA256 for ByteString message.");
        return digest(SHA256Digest, message);
    }

    /**
     * Compute the SHA-256 hash of a message.
     *
     * @param message The message to be hashed.
     * @return The hashed message.
     */
    public static Hash digestSHA256(@NotNull Hash message) {
        LOGGER.fine("Digest SHA256 for Hash message.");
        return digestSHA256(message.getValue());
    }

    private static @NotNull Hash digest(@NotNull MessageDigest messageDigest,
                                        @NotNull ByteString message) {
        LOGGER.log(Level.FINE, "Digest for ByteString message using MessageDigest: {0}", messageDigest.getAlgorithm());
        Hash hash = new Hash(ByteString.copyFrom(messageDigest.digest(message.toByteArray())));
        LOGGER.log(Level.FINEST, () -> MessageFormat.format("{0} ( {1} ) = {2}", new Object[]{messageDigest.getAlgorithm(), toHexString(message), toHexString(hash.getValue())}));
        return hash;
    }

}
