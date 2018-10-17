package org.brabocoin.brabocoin.util;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import com.google.protobuf.Parser;
import net.badata.protobuf.converter.Converter;
import org.brabocoin.brabocoin.model.proto.ProtoBuilder;
import org.brabocoin.brabocoin.model.proto.ProtoModel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author Sten Wessel
 */
public class ProtoConverter {

    private static final Converter CONVERTER = Converter.create();

    public static <D extends ProtoModel<D>, B extends ProtoBuilder<D>, P extends Message> @Nullable D parseProtoValue(@Nullable ByteString value,
                                                                                             @NotNull Class<B> domainClassBuilder,
                                                                                             @NotNull Parser<P> parser) throws InvalidProtocolBufferException {
        if (value == null) {
            return null;
        }

        P proto = parser.parseFrom(value);

        return toDomain(proto, domainClassBuilder);
    }

    public static <D extends ProtoModel<D>, P extends Message> P toProto(D domainObject, Class<P> protoClass) {
        return CONVERTER.toProtobuf(protoClass, domainObject);
    }

    public static <D extends ProtoModel<D>, B extends ProtoBuilder<D>, P extends Message> D toDomain(P protoObject, Class<B> domainClassBuilder) {
        B builder = CONVERTER.toDomain(domainClassBuilder, protoObject);

        if (builder == null) {
            return null;
        }

        return builder.build();
    }
}
