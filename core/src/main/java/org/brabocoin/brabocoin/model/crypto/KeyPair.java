package org.brabocoin.brabocoin.model.crypto;

import net.badata.protobuf.converter.annotation.ProtoClass;
import net.badata.protobuf.converter.annotation.ProtoField;
import org.brabocoin.brabocoin.crypto.PublicKey;
import org.brabocoin.brabocoin.model.Hash;
import org.brabocoin.brabocoin.model.proto.ProtoBuilder;
import org.brabocoin.brabocoin.model.proto.ProtoModel;
import org.brabocoin.brabocoin.model.proto.Secp256k1PublicKeyByteStringConverter;
import org.brabocoin.brabocoin.proto.model.BrabocoinProtos;
import org.jetbrains.annotations.NotNull;

/**
 * Represents a public and private key pair
 */
@ProtoClass(BrabocoinProtos.KeyPair.class)
public class KeyPair implements ProtoModel<KeyPair> {
    @ProtoField(converter = Secp256k1PublicKeyByteStringConverter.class)
    private PublicKey publicKey;
    @ProtoField
    private PrivateKey privateKey;

    public KeyPair(PublicKey publicKey, PrivateKey privateKey) {
        this.publicKey = publicKey;
        this.privateKey = privateKey;
    }

    @Override
    public Class<? extends ProtoBuilder> getBuilder() {
        return Builder.class;
    }

    public PublicKey getPublicKey() {
        return publicKey;
    }

    public PrivateKey getPrivateKey() {
        return privateKey;
    }

    @ProtoClass(BrabocoinProtos.KeyPair.class)
    public static class Builder implements ProtoBuilder<KeyPair> {

        @ProtoField(converter = Secp256k1PublicKeyByteStringConverter.class)
        private PublicKey publicKey;
        @ProtoField
        private PrivateKey.Builder privateKey;

        @Override
        public KeyPair build() {
            return new KeyPair(publicKey, privateKey.build());
        }

        public void setPublicKey(PublicKey publicKey) {
            this.publicKey = publicKey;
        }

        public void setPrivateKey(PrivateKey.Builder privateKey) {
            this.privateKey = privateKey;
        }
    }
}
