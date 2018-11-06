package org.brabocoin.brabocoin.model.proto;

import com.google.protobuf.ByteString;
import net.badata.protobuf.converter.type.TypeConverter;

import java.math.BigInteger;

/**
 * @author Sten Wessel
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
