package org.brabocoin.brabocoin.model.proto;

import com.google.protobuf.ByteString;
import net.badata.protobuf.converter.type.TypeConverter;
import org.brabocoin.brabocoin.crypto.EllipticCurve;
import org.brabocoin.brabocoin.crypto.PublicKey;

/**
 * Converts a {@link PublicKey} to a {@link ByteString} using the {@code secp256k1} elliptic curve.
 */
public class Secp256k1PublicKeyByteStringConverter implements TypeConverter<PublicKey, ByteString> {

    @Override
    public PublicKey toDomainValue(Object instance) {
        return PublicKey.fromCompressed(((ByteString)instance), EllipticCurve.secp256k1());
    }

    @Override
    public ByteString toProtobufValue(Object instance) {
        return ((PublicKey)instance).toCompressed();
    }
}
