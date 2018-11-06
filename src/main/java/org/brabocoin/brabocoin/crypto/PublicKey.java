package org.brabocoin.brabocoin.crypto;

import com.google.protobuf.ByteString;
import org.bouncycastle.math.ec.ECPoint;
import org.brabocoin.brabocoin.model.Hash;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.logging.Logger;


/**
 * Represents a public key from a point on an Elliptic Curve.
 *
 * @see EllipticCurve
 */
public class PublicKey {

    private static final Logger LOGGER = Logger.getLogger(PublicKey.class.getName());

    /**
     * The point on the elliptic curve corresponding to the public key.
     */
    private final @NotNull ECPoint point;

    /**
     * Construct a public key from compressed form, on the provided elliptic curve.
     *
     * @param compressed
     *     The compressed form of the public key.
     * @param curve
     *     The curve on which the public key was generated.
     * @return The public key.
     * @throws IllegalArgumentException
     *     when the public key could not be decoded.
     */
    @Contract("_, _ -> new")
    public static @NotNull PublicKey fromCompressed(@NotNull ByteString compressed,
                                                    @NotNull EllipticCurve curve) throws IllegalArgumentException {
        return new PublicKey(curve.decodePoint(compressed));
    }

    /**
     * Construct a public key from a point on an elliptic curve.
     *
     * @param point
     *     The point on the elliptic curve.
     */
    public PublicKey(@NotNull ECPoint point) {
        this.point = point;
    }

    /**
     * Get the point on the elliptic curve the public key corresponds to.
     *
     * @return The point on the elliptic curve.
     */
    public @NotNull ECPoint getPoint() {
        return point;
    }

    /**
     * Convert the public key to compressed encoded format.
     *
     * @return The compressed public key.
     */
    public @NotNull ByteString toCompressed() {
        return ByteString.copyFrom(point.getEncoded(true));
    }

    /**
     * Computes the hash of the public key.
     *
     * @return The hash of the public key.
     */
    public @NotNull Hash computeHash() {
        return Hashing.digestRIPEMD160(Hashing.digestSHA256(toCompressed()));
    }

    /**
     * Computes the address the public key corresponds to.
     * <p>
     * The address is a human-readable format that is derived from the hash of the public key.
     *
     * @return The address corresponding to the public key.
     */
    public @NotNull String computeAddress() {
        // TODO: implement Base58Check encoding of hash
        return "";
    }
}
