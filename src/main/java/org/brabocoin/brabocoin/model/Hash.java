package org.brabocoin.brabocoin.model;

import com.google.protobuf.ByteString;
import net.badata.protobuf.converter.annotation.ProtoClass;
import net.badata.protobuf.converter.annotation.ProtoField;
import org.brabocoin.brabocoin.proto.model.BrabocoinProtos;
import org.jetbrains.annotations.NotNull;

/**
 * Represents a hash value.
 */
@ProtoClass(BrabocoinProtos.Hash.class)
public class Hash {

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
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Hash hash = (Hash)o;

        return value.equals(hash.value);
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }

    @ProtoClass(BrabocoinProtos.Hash.class)
    public static class Builder {

        @ProtoField
        private ByteString value;

        public Builder setValue(ByteString value) {
            this.value = value;
            return this;
        }

        /**
         * Creates the {@link Hash} object.
         *
         * @return The hash.
         */
        public Hash createHash() {
            return new Hash(value);
        }
    }
}
