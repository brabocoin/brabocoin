package org.brabocoin.brabocoin.validation;

import com.google.protobuf.ByteString;
import org.bouncycastle.asn1.sec.SECNamedCurves;
import org.bouncycastle.asn1.x9.X9ECParameters;
import org.bouncycastle.crypto.CipherParameters;
import org.bouncycastle.crypto.params.ECDomainParameters;
import org.bouncycastle.crypto.params.ECPublicKeyParameters;
import org.bouncycastle.crypto.signers.ECDSASigner;
import org.bouncycastle.math.ec.ECPoint;
import org.brabocoin.brabocoin.model.Hash;
import org.brabocoin.brabocoin.model.Signature;
import org.jetbrains.annotations.NotNull;

import java.util.logging.Logger;

/**
 * Digital signature creator and verifier.
 */
public class Signer {

    private static final Logger LOGGER = Logger.getLogger(Signer.class.getName());

    private static final X9ECParameters CURVE = SECNamedCurves.getByName("secp256k1");
    private static final ECDomainParameters DOMAIN = new ECDomainParameters(CURVE.getCurve(),
        CURVE.getG(),
        CURVE.getN(),
        CURVE.getH()
    );

    private final @NotNull ECDSASigner signer;

    public Signer(@NotNull ECDSASigner signer) {
        this.signer = signer;
    }

    public boolean verifySignature(@NotNull Signature signature, @NotNull Hash address,
                                   @NotNull ByteString message) {

        ECPoint publicKey = CURVE.getCurve().decodePoint(signature.getPublicKey().toByteArray());
        CipherParameters pubKeyParams = new ECPublicKeyParameters(publicKey, DOMAIN);

        signer.init(false, pubKeyParams);

        boolean valid = signer.verifySignature(message.toByteArray(),
            signature.getR(),
            signature.getS()
        );

        // TODO: verify that the pub key in the signature corresponds to the given address.

        return valid;
    }
}
