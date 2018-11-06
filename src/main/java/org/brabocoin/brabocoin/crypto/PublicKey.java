package org.brabocoin.brabocoin.crypto;

import com.google.protobuf.ByteString;
import org.bouncycastle.math.ec.ECPoint;
import org.brabocoin.brabocoin.model.Hash;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;


/**
 * @author Sten Wessel
 */
public class PublicKey {

    private final @NotNull ECPoint point;

    @Contract("_, _ -> new")
    public static @NotNull PublicKey fromCompressed(@NotNull ByteString compressed,
                                                    @NotNull EllipticCurve curve) {
        return new PublicKey(curve.decodePoint(compressed));
    }

    public PublicKey(@NotNull ECPoint point) {
        this.point = point;
    }

    public @NotNull ECPoint getPoint() {
        return point;
    }

    public @NotNull ByteString toCompressed() {
        return ByteString.copyFrom(point.getEncoded(true));
    }

    public @NotNull Hash toHash() {
        return Hashing.digestRIPEMD160(Hashing.digestSHA256(toCompressed()));
    }

    public @NotNull String toAddress() {
        // TODO: implement Base58Check encoding of hash
        return "";
    }
}
