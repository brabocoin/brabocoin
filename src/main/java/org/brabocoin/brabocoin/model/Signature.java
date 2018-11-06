package org.brabocoin.brabocoin.model;

import com.google.protobuf.ByteString;
import net.badata.protobuf.converter.annotation.ProtoClass;
import net.badata.protobuf.converter.annotation.ProtoField;
import org.brabocoin.brabocoin.model.proto.BigIntegerByteStringConverter;
import org.brabocoin.brabocoin.model.proto.ProtoBuilder;
import org.brabocoin.brabocoin.model.proto.ProtoModel;
import org.brabocoin.brabocoin.proto.model.BrabocoinProtos;
import org.jetbrains.annotations.NotNull;

import java.math.BigInteger;

@ProtoClass(BrabocoinProtos.Signature.class)
public class Signature implements ProtoModel<Signature> {

    @ProtoField(converter = BigIntegerByteStringConverter.class)
    private final @NotNull BigInteger r;

    @ProtoField(converter = BigIntegerByteStringConverter.class)
    private final @NotNull BigInteger s;

    @ProtoField
    private final @NotNull ByteString publicKey;

    public Signature(@NotNull BigInteger r, @NotNull BigInteger s, @NotNull ByteString publicKey) {
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

    public @NotNull ByteString getPublicKey() {
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

        @ProtoField
        private ByteString publicKey;

        public Builder setR(@NotNull BigInteger r) {
            this.r = r;
            return this;
        }

        public Builder setS(@NotNull BigInteger s) {
            this.s = s;
            return this;
        }

        public Builder setPublicKey(@NotNull ByteString publicKey) {
            this.publicKey = publicKey;
            return this;
        }

        @Override
        public Signature build() {
            return new Signature(r, s, publicKey);
        }
    }
}
