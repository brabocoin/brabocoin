package org.brabocoin.brabocoin.util;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import com.google.protobuf.Parser;
import net.badata.protobuf.converter.Configuration;
import net.badata.protobuf.converter.Converter;
import org.brabocoin.brabocoin.model.proto.ProtoBuilder;
import org.brabocoin.brabocoin.model.proto.ProtoModel;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author Sten Wessel
 */
public class ProtoConverter {

    private static final Converter CONVERTER = Converter.create(
            Configuration.builder().withInheritedFields().build()
    );

    @Contract("null, _, _ -> null")
    public static <D extends ProtoModel<D>, B extends ProtoBuilder<D>, P extends Message> @Nullable D parseProtoValue(@Nullable ByteString value, @NotNull Class<B> domainClassBuilder, @NotNull Parser<P> parser) throws InvalidProtocolBufferException {
        if (value == null) {
            return null;
        }

        P proto = parser.parseFrom(value);

        return toDomain(proto, domainClassBuilder);
    }


    public static <M extends D, D extends ProtoModel<D>, B extends ProtoBuilder<D>, P extends Message> @Nullable M toDomain(P protoObject, Class<B> domainClassBuilder) {
        B builder = CONVERTER.toDomain(domainClassBuilder, protoObject);

        if (builder == null) {
            return null;
        }

        return builder.build();
    }

    @Contract("null, _ -> null")
    public static <D extends ProtoModel<D>, P extends Message> P toProto(D domainObject,
                                                                         Class<P> protoClass) {
        return CONVERTER.toProtobuf(protoClass, domainObject);
    }

    /**
     * Convert a domain object to the byte representation of the converted proto object.
     *
     * @param domainObject
     *     The domain object instance.
     * @param protoClass
     *     The generated proto class.
     * @param <D>
     *     The proto model class.
     * @param <P>
     *     The generated proto class.
     * @return The raw bytes of the converted proto object, or {@code null} if the object could
     * not be converted.
     */
    public static <D extends ProtoModel<D>, P extends Message> ByteString toProtoBytes(D domainObject, Class<P> protoClass) {
        P proto = toProto(domainObject, protoClass);
        if (proto == null) {
            return null;
        }

        return proto.toByteString();
    }
}
