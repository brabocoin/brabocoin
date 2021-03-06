package org.brabocoin.brabocoin.model.crypto;

import net.badata.protobuf.converter.annotation.ProtoClass;
import net.badata.protobuf.converter.annotation.ProtoField;
import org.brabocoin.brabocoin.crypto.PublicKey;
import org.brabocoin.brabocoin.model.proto.BigIntegerByteStringConverter;
import org.brabocoin.brabocoin.model.proto.ProtoBuilder;
import org.brabocoin.brabocoin.model.proto.ProtoModel;
import org.brabocoin.brabocoin.model.proto.Secp256k1PublicKeyByteStringConverter;
import org.brabocoin.brabocoin.proto.model.BrabocoinProtos;
import org.jetbrains.annotations.NotNull;

import java.math.BigInteger;

@ProtoClass(BrabocoinProtos.Signature.class)
public class Signature implements ProtoModel<Signature> {

    @ProtoField(converter = BigIntegerByteStringConverter.class)
    private final @NotNull BigInteger r;

    @ProtoField(converter = BigIntegerByteStringConverter.class)
    private final @NotNull BigInteger s;

    @ProtoField(converter = Secp256k1PublicKeyByteStringConverter.class)
    private final @NotNull PublicKey publicKey;

    public Signature(@NotNull BigInteger r, @NotNull BigInteger s, @NotNull PublicKey publicKey) {
        this.r = r;
        this.s = s;
        this.publicKey = publicKey;
    }

    public @NotNull BigInteger getR() {
        return r;
    }

    public @NotNull BigInteger getS() {
        return s;
    }

    public @NotNull PublicKey getPublicKey() {
        return publicKey;
    }

    @Override
    public Class<? extends ProtoBuilder> getBuilder() {
        return Builder.class;
    }

    @ProtoClass(BrabocoinProtos.Signature.class)
    public static class Builder implements ProtoBuilder<Signature> {

        @ProtoField(converter = BigIntegerByteStringConverter.class)
        private BigInteger r;

        @ProtoField(converter = BigIntegerByteStringConverter.class)
        private BigInteger s;

        @ProtoField(converter = Secp256k1PublicKeyByteStringConverter.class)
        private PublicKey publicKey;

        public Builder setR(@NotNull BigInteger r) {
            this.r = r;
            return this;
        }

        public Builder setS(@NotNull BigInteger s) {
            this.s = s;
            return this;
        }

        public Builder setPublicKey(@NotNull PublicKey publicKey) {
            this.publicKey = publicKey;
            return this;
        }

        @Override
        public Signature build() {
            return new Signature(r, s, publicKey);
        }
    }
}
