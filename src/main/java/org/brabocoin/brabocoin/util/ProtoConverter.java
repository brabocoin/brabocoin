package org.brabocoin.brabocoin.util;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import com.google.protobuf.Parser;
import net.badata.protobuf.converter.Converter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author Sten Wessel
 */
public class ProtoConverter {

    private static final Converter CONVERTER = Converter.create();

    public static <D, P extends Message> @Nullable D parseProtoValue(@Nullable ByteString value,
                                                               @NotNull Class<D> domainClass,
                                                               @NotNull Parser<P> parser) throws InvalidProtocolBufferException {
        if (value == null) {
            return null;
        }

        P proto = parser.parseFrom(value);

        return toDomain(proto, domainClass);
    }

    public static <D, P extends Message> P toProto(D domainObject, Class<P> protoClass) {
        return CONVERTER.toProtobuf(protoClass, domainObject);
    }

    public static <D, P extends Message> D toDomain(P protoObject, Class<D> domainClass) {
        return CONVERTER.toDomain(domainClass, protoObject);
    }
}
