package org.brabocoin.brabocoin.model.proto;

import com.google.protobuf.ByteString;
import net.badata.protobuf.converter.type.TypeConverter;

import java.math.BigInteger;

/**
 * Converts a {@link BigInteger} to a {@link ByteString}.
 */
public class BigIntegerByteStringConverter implements TypeConverter<BigInteger, ByteString> {

    @Override
    public BigInteger toDomainValue(Object instance) {
        return new BigInteger(((ByteString)instance).toByteArray());
    }

    @Override
    public ByteString toProtobufValue(Object instance) {
        return ByteString.copyFrom(((BigInteger)instance).toByteArray());
    }
}
