package org.brabocoin.brabocoin.crypto;

import com.google.protobuf.ByteString;
import org.bouncycastle.math.ec.ECPoint;
import org.brabocoin.brabocoin.util.Base58Check;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.logging.Logger;


/**
 * Represents a public key from a point on an Elliptic Curve.
 *
 * @see EllipticCurve
 */
public class PublicKey {

    private static final Logger LOGGER = Logger.getLogger(PublicKey.class.getName());

    /**
     * Prefix of the generated Base58check address (0x00, or 1 encoded in Base58).
     */
    private static final ByteString ADDRESS_PREFIX = ByteString.copyFrom(new byte[] {0x00});

    /**
     * The point on the elliptic curve corresponding to the public key.
     */
    private final @NotNull ECPoint point;

    /**
     * Cached hash
     */
    private org.brabocoin.brabocoin.model.Hash hash;

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
     * Computes the address the public key corresponds to.
     * <p>
     * The address is a human-readable format that is derived from the hash of the public key.
     *
     * @param hash
     *     The hash of the public key.
     * @return The address corresponding to the public key.
     */
    public static @NotNull String getBase58AddressFromHash(@NotNull org.brabocoin.brabocoin.model.Hash hash) {
        return Base58Check.encode(ADDRESS_PREFIX.concat(hash.getValue()));
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
    public @NotNull org.brabocoin.brabocoin.model.Hash getHash() {
        if (hash == null) {
            hash = Hashing.digestRIPEMD160(Hashing.digestSHA256(toCompressed()));
        }

        return hash;
    }

    /**
     * Computes the address the public key corresponds to.
     * <p>
     * The address is a human-readable format that is derived from the hash of the public key.
     *
     * @return The address corresponding to the public key.
     */
    public @NotNull String getBase58Address() {
        return getBase58AddressFromHash(getHash());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        PublicKey publicKey = (PublicKey)o;
        return Objects.equals(point, publicKey.point);
    }

    @Override
    public int hashCode() {
        return Objects.hash(point);
    }

    @Override
    public String toString() {
        return this.getBase58Address();
    }
}
