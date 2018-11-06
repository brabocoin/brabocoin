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
import org.brabocoin.brabocoin.util.EllipticCurve;
import org.jetbrains.annotations.NotNull;

import java.math.BigInteger;
import java.util.logging.Logger;

/**
 * Digital signature creator and verifier.
 */
public class Signer {

    private static final Logger LOGGER = Logger.getLogger(Signer.class.getName());

    private final @NotNull EllipticCurve curve;
    private final @NotNull ECDSASigner signer;

    public Signer(@NotNull EllipticCurve curve) {
        this.curve = curve;
        this.signer = new ECDSASigner();
    }

    public boolean verifySignature(@NotNull Signature signature, @NotNull Hash address,
                                   @NotNull ByteString message) {

        ECPoint publicKey = curve.decodePoint(signature.getPublicKey());
        CipherParameters parameters = new ECPublicKeyParameters(publicKey, curve.getDomain());

        signer.init(false, parameters);

        boolean valid = signer.verifySignature(message.toByteArray(),
            signature.getR(),
            signature.getS()
        );

        // TODO: verify that the pub key in the signature corresponds to the given address.

        return valid;
    }

    public @NotNull Signature signMessage(@NotNull ByteString message, @NotNull BigInteger privateKey) {
        // Check if private key is in bounds
        if (!BigIntegerUtil.isInRangeExclusive(privateKey, BigInteger.ZERO, curve.getDomain().getN())) {
            throw new IllegalArgumentException("Private key is not within range (0, n).");
        }

        CipherParameters parameters = new ECPrivateKeyParameters(privateKey, curve.getDomain());

        signer.init(true, parameters);

        BigInteger[] rAndS = signer.generateSignature(message.toByteArray());

        ECPoint publicKey = curve.getPublicKeyFromPrivateKey(privateKey);
        ByteString compressed = ByteString.copyFrom(publicKey.getEncoded(true));

        return new Signature(rAndS[0], rAndS[1], compressed);
    }
}
