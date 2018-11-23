package org.brabocoin.brabocoin.crypto;

import com.google.protobuf.ByteString;
import org.bouncycastle.crypto.CipherParameters;
import org.bouncycastle.crypto.params.ECPrivateKeyParameters;
import org.bouncycastle.crypto.params.ECPublicKeyParameters;
import org.bouncycastle.crypto.signers.ECDSASigner;
import org.bouncycastle.math.ec.ECPoint;
import org.brabocoin.brabocoin.model.Hash;
import org.brabocoin.brabocoin.model.Signature;
import org.brabocoin.brabocoin.util.BigIntegerUtil;
import org.jetbrains.annotations.NotNull;

import java.math.BigInteger;
import java.text.MessageFormat;
import java.util.logging.Logger;

import static org.brabocoin.brabocoin.util.ByteUtil.toHexString;

/**
 * Digital signature signer and verifier.
 * <p>
 * Encapsulates crypto library signer implementation.
 */
public class Signer {

    private static final Logger LOGGER = Logger.getLogger(Signer.class.getName());

    /**
     * The elliptic curve used for signing.
     */
    private final @NotNull EllipticCurve curve;

    /**
     * Actual signer implementation.
     */
    private final @NotNull ECDSASigner signer;

    /**
     * Create a new signing using the given elliptic curve.
     *
     * @param curve
     *     The elliptic curve.
     */
    public Signer(@NotNull EllipticCurve curve) {
        this.curve = curve;
        this.signer = new ECDSASigner();
    }

    /**
     * Sign a message with the given private key.
     *
     * @param message
     *     The message to sign.
     * @param privateKey
     *     The private key used to sign the message.
     * @return The signature, containing the private key, of the message.
     */
    public @NotNull Signature signMessage(@NotNull ByteString message,
                                          @NotNull BigInteger privateKey) {
        LOGGER.fine("Signing a message.");

        // Check if private key is in bounds
        boolean inRange = BigIntegerUtil.isInRangeExclusive(
            privateKey,
            BigInteger.ZERO,
            curve.getDomain().getN()
        );

        if (!inRange) {
            LOGGER.warning("Private key is out of valid range.");
            throw new IllegalArgumentException("Private key is not within range (0, n).");
        }

        CipherParameters parameters = new ECPrivateKeyParameters(privateKey, curve.getDomain());

        LOGGER.fine("Initializing crypto library signer.");
        signer.init(true, parameters);

        LOGGER.fine("Generating crypto library signature.");
        BigInteger[] rAndS = signer.generateSignature(message.toByteArray());

        BigInteger r = rAndS[0];
        BigInteger s = rAndS[1];

        LOGGER.fine("Generating public key from private key.");
        PublicKey publicKey = curve.getPublicKeyFromPrivateKey(privateKey);

        LOGGER.finest(() -> MessageFormat.format(
            "Compressed public key={0}, r={1}, s={2}",
            toHexString(publicKey.toCompressed()),
            r,
            s
        ));

        return new Signature(r, s, publicKey);
    }

    /**
     * Verify a signature.
     * <p>
     * A signature is only valid if the public key in the signature corresponds to the given
     * address.
     *
     * @param signature
     *     The signature to verify.
     * @param publicKeyHash
     *     The hash of the public key that is used in the signature.
     * @param message
     *     The message that is signed.
     * @return Whether the signature is valid.
     */
    public boolean verifySignature(@NotNull Signature signature, @NotNull Hash publicKeyHash,
                                   @NotNull ByteString message) {
        LOGGER.fine("Verifying a signature.");

        LOGGER.fine("Checking if public key hash corresponds to public key in signature.");
        if (!signature.getPublicKey().getHash().equals(publicKeyHash)) {
            LOGGER.fine("Public key hash does not match signature public key: signature invalid.");
            return false;
        }

        LOGGER.fine("Public key hash matches signature public key.");

        ECPoint publicKeyPoint = signature.getPublicKey().getPoint();
        CipherParameters parameters = new ECPublicKeyParameters(publicKeyPoint, curve.getDomain());

        LOGGER.fine("Initializing crypto library signer.");
        signer.init(false, parameters);

        LOGGER.fine("Verifying crypto library signature.");

        return signer.verifySignature(
            message.toByteArray(),
            signature.getR(),
            signature.getS()
        );
    }
}
