package org.brabocoin.brabocoin.util;

import com.google.protobuf.ByteString;
import org.bouncycastle.asn1.sec.SECNamedCurves;
import org.bouncycastle.asn1.x9.X9ECParameters;
import org.bouncycastle.crypto.params.ECDomainParameters;
import org.bouncycastle.math.ec.ECPoint;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.math.BigInteger;

/**
 * @author Sten Wessel
 */
public class EllipticCurve {

    private final @NotNull ECDomainParameters domain;

    @Contract(" -> new")
    public static @NotNull EllipticCurve secp256k1() {
        return new EllipticCurve(SECNamedCurves.getByName("secp256k1"));
    }

    public EllipticCurve(@NotNull X9ECParameters parameters) {
        this.domain = new ECDomainParameters(
            parameters.getCurve(),
            parameters.getG(),
            parameters.getN(),
            parameters.getH()
        );
    }

    public @NotNull ECPoint getPublicKeyFromPrivateKey(@NotNull BigInteger privateKey) {
        return domain.getG().multiply(privateKey);
    }

    public @NotNull ECPoint decodePoint(@NotNull ByteString point) {
        return domain.getCurve().decodePoint(point.toByteArray());
    }

    public @NotNull ECDomainParameters getDomain() {
        return domain;
    }
}
