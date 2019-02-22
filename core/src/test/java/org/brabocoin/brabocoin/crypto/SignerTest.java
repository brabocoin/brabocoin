package org.brabocoin.brabocoin.crypto;

import com.google.protobuf.ByteString;
import org.brabocoin.brabocoin.model.crypto.Signature;
import org.brabocoin.brabocoin.testutil.Simulation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test {@link Signer}.
 */
class SignerTest {

    private static final ByteString ZERO = ByteString.copyFrom(new byte[] {0});
    private static final EllipticCurve CURVE = EllipticCurve.secp256k1();

    private Signer signer;

    @BeforeEach
    void setUp() {
        signer = new Signer(CURVE);
    }

    @Test
    void signMessageInvalidPrivateKeyZero() {
        assertThrows(IllegalArgumentException.class,
            () -> signer.signMessage(ZERO, BigInteger.ZERO)
        );
    }

    @Test
    void signMessageInvalidPrivateKeyN() {
        assertThrows(IllegalArgumentException.class,
            () -> signer.signMessage(ZERO, CURVE.getDomain().getN())
        );
    }

    @Test
    void signMessageInvalidPrivateKeyP() {
        assertThrows(IllegalArgumentException.class,
            () -> signer.signMessage(ZERO, CURVE.getDomain().getCurve().getField().getCharacteristic())
        );
    }

    @Test
    void verifySignaturePointAtInfinity() {
        Signature signature = new Signature(
            BigInteger.ZERO,
            BigInteger.ZERO,
            new PublicKey(CURVE.getDomain().getCurve().getInfinity())
        );
        assertThrows(
            IllegalArgumentException.class,
            () -> signer.verifySignature(signature, ZERO)
        );
    }

    @Test
    void verifySignatureAttackSZero() {
        PublicKey publicKey = CURVE.getPublicKeyFromPrivateKey(BigInteger.TEN);

        Signature signature = new Signature(BigInteger.ZERO, BigInteger.ZERO, publicKey);
        boolean valid = signer.verifySignature(signature, ZERO);

        assertFalse(valid);
    }

    @Test
    void verifySignatureAttackSNotZero() {
        PublicKey publicKey = CURVE.getPublicKeyFromPrivateKey(BigInteger.TEN);

        Signature signature = new Signature(BigInteger.ZERO, BigInteger.TEN, publicKey);
        boolean valid = signer.verifySignature(signature, ZERO);

        assertFalse(valid);
    }

    @Test
    void verifySignatureAttackSNotZeroRN() {
        PublicKey publicKey = CURVE.getPublicKeyFromPrivateKey(BigInteger.TEN);

        Signature signature = new Signature(CURVE.getDomain().getN(), BigInteger.TEN, publicKey);
        boolean valid = signer.verifySignature(signature, ZERO);

        assertFalse(valid);
    }

    @Test
    void signAndVerifyMessage() {
        BigInteger privateKey = new BigInteger(140, new Random());
        ByteString message = ByteString.copyFromUtf8("Houtjes en touwtjes");

        Signature signature = signer.signMessage(message, privateKey);

        assertTrue(signer.verifySignature(signature, message));
    }

    @Test
    void signAndVerifyInvalidHash() {
        BigInteger privateKey = new BigInteger(140, new Random());
        ByteString message = ByteString.copyFromUtf8("Houtjes en touwtjes");

        Signature signature = signer.signMessage(message, privateKey);

        assertFalse(signer.verifySignaturePublicKey(signature, Simulation.randomHash()));
    }

    @Test
    void signAndVerifyCorruptedMessage() {
        BigInteger privateKey = new BigInteger(140, new Random());
        ByteString message = ByteString.copyFromUtf8("Houtjes en touwtjes");
        ByteString corruptedMessage = ByteString.copyFromUtf8("Little woods, little ropes");

        Signature signature = signer.signMessage(message, privateKey);

        assertFalse(signer.verifySignature(signature, corruptedMessage));
    }

    @Test
    void signAndVerifyCorruptedR() {
        BigInteger privateKey = new BigInteger(140, new Random());
        ByteString message = ByteString.copyFromUtf8("Houtjes en touwtjes");

        Signature signature = signer.signMessage(message, privateKey);

        Signature corruptedSignature = new Signature(
            signature.getR().add(BigInteger.ONE),
            signature.getS(),
            signature.getPublicKey()
        );

        assertFalse(signer.verifySignature(corruptedSignature, message));
    }

    @Test
    void signAndVerifyCorruptedS() {
        BigInteger privateKey = new BigInteger(140, new Random());
        ByteString message = ByteString.copyFromUtf8("Houtjes en touwtjes");

        Signature signature = signer.signMessage(message, privateKey);

        Signature corruptedSignature = new Signature(
            signature.getR(),
            signature.getS().add(BigInteger.ONE),
            signature.getPublicKey()
        );

        assertFalse(signer.verifySignature(corruptedSignature, message));
    }

    @Test
    void signAndVerifyCorruptedPublicKey() {
        BigInteger privateKey = new BigInteger(140, new Random());
        ByteString message = ByteString.copyFromUtf8("Houtjes en touwtjes");

        Signature signature = signer.signMessage(message, privateKey);

        PublicKey corruptedPublicKey = CURVE.getPublicKeyFromPrivateKey(privateKey.add(BigInteger.ONE));

        Signature corruptedSignature = new Signature(
            signature.getR(),
            signature.getS().add(BigInteger.ONE),
            corruptedPublicKey
        );

        assertFalse(signer.verifySignaturePublicKey(corruptedSignature, signature.getPublicKey().getHash()));
    }
}
