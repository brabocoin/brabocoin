package org.brabocoin.brabocoin.model;

import com.google.protobuf.ByteString;
import net.badata.protobuf.converter.annotation.ProtoClass;
import net.badata.protobuf.converter.annotation.ProtoField;
import org.brabocoin.brabocoin.model.proto.ProtoBuilder;
import org.brabocoin.brabocoin.model.proto.ProtoModel;
import org.brabocoin.brabocoin.proto.model.BrabocoinProtos;
import org.jetbrains.annotations.NotNull;

/**
 * Represents a hash value.
 */
@ProtoClass(BrabocoinProtos.Hash.class)
public class Hash implements ProtoModel<Hash> {

    /**
     * Value of the hash.
     */
    @ProtoField
    private final @NotNull ByteString value;

    /**
     * Creates a new Hash.
     *
     * @param value
     *         The value of the hash.
     */
    public Hash(@NotNull ByteString value) {
        this.value = value;
    }

    public @NotNull ByteString getValue() {
        return value;
    }

    @Override
    public Class<? extends ProtoBuilder> getBuilder() {
        return Builder.class;
    }

    @ProtoClass(BrabocoinProtos.Hash.class)
    public static class Builder implements ProtoBuilder<Hash> {

        @ProtoField
        private ByteString value;

        public Builder setValue(ByteString value) {
            this.value = value;
            return this;
        }

        @Override
        public Hash build() {
            return new Hash(value);
        }
    }
}
