package org.brabocoin.brabocoin.crypto;

import com.google.protobuf.ByteString;
import org.bouncycastle.asn1.sec.SECNamedCurves;
import org.bouncycastle.asn1.x9.X9ECParameters;
import org.bouncycastle.crypto.params.ECDomainParameters;
import org.bouncycastle.math.ec.ECPoint;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.math.BigInteger;
import java.util.logging.Logger;

/**
 * Elliptic curve representation for crypto usage.
 */
public class EllipticCurve {

    private static final Logger LOGGER = Logger.getLogger(EllipticCurve.class.getName());

    /**
     * Domain parameters.
     */
    private final @NotNull ECDomainParameters domain;

    /**
     * Create a new elliptic curve from the {@code secp256k1} standard curve definition.
     *
     * @return The {@code secp256k1} elliptic curve.
     */
    @Contract(" -> new")
    public static @NotNull EllipticCurve secp256k1() {
        return new EllipticCurve(SECNamedCurves.getByName("secp256k1"));
    }

    /**
     * Create a new elliptic curve from the given parameters.
     *
     * @param parameters
     *     The elliptic curve parameters.
     */
    public EllipticCurve(@NotNull X9ECParameters parameters) {
        this.domain = new ECDomainParameters(parameters.getCurve(),
            parameters.getG(),
            parameters.getN(),
            parameters.getH()
        );
    }

    /**
     * Generate a public key point from a private key.
     *
     * @param privateKey
     *     The private key.
     * @return The public key point.
     */
    public @NotNull PublicKey getPublicKeyFromPrivateKey(@NotNull BigInteger privateKey) {
        LOGGER.fine("Generating public key from private key.");
        return new PublicKey(domain.getG().multiply(privateKey));
    }

    /**
     * Decode an elliptic curve point from binary representation.
     *
     * @param point
     *     The point to decode.
     * @return The decoded point.
     */
    public @NotNull ECPoint decodePoint(@NotNull ByteString point) {
        LOGGER.fine("Decoding point from ByteString.");
        return domain.getCurve().decodePoint(point.toByteArray());
    }

    /**
     * Get the domain parameters.
     *
     * @return The domain parameters.
     */
    public @NotNull ECDomainParameters getDomain() {
        return domain;
    }

}
