package org.brabocoin.brabocoin.model;

import com.google.protobuf.ByteString;
import net.badata.protobuf.converter.annotation.ProtoClass;
import net.badata.protobuf.converter.annotation.ProtoField;
import org.bouncycastle.util.Arrays;
import org.brabocoin.brabocoin.model.proto.ProtoBuilder;
import org.brabocoin.brabocoin.model.proto.ProtoModel;
import org.brabocoin.brabocoin.proto.model.BrabocoinProtos;
import org.brabocoin.brabocoin.util.ByteUtil;
import org.jetbrains.annotations.NotNull;

/**
 * Represents a hash value.
 */
@ProtoClass(BrabocoinProtos.Hash.class)
public class Hash implements ProtoModel<Hash>, Comparable<Hash> {

    /**
     * Value of the hash.
     */
    @ProtoField
    private final @NotNull ByteString value;

    /**
     * Creates a new Hash.
     *
     * @param value
     *     The value of the hash.
     */
    public Hash(@NotNull ByteString value) {
        this.value = value;
    }

    /**
     * Returns a new hash object representing the concatenated value of the current hash and the
     * provided hash.
     *
     * @param hash
     *     The hash to concatenate to this hash.
     * @return The concatenated hash.
     */
    public @NotNull Hash concat(@NotNull Hash hash) {
        return new Hash(this.getValue().concat(hash.getValue()));
    }

    public @NotNull ByteString getValue() {
        return value;
    }

    @Override
    public Class<? extends ProtoBuilder> getBuilder() {
        return Builder.class;
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

    @Override
    public int compareTo(@NotNull Hash o) {
        int lengthDifference = value.size() - o.value.size();

        byte[] thisArray;
        byte[] otherArray;

        if (lengthDifference > 0) {
            thisArray = value.toByteArray();
            otherArray = ByteString.copyFrom(new byte[lengthDifference])
                .concat(o.value)
                .toByteArray();
        }
        else if (lengthDifference < 0) {
            thisArray = ByteString.copyFrom(new byte[-lengthDifference])
                .concat(value)
                .toByteArray();
            otherArray = o.value.toByteArray();
        }
        else {
            thisArray = value.toByteArray();
            otherArray = o.value.toByteArray();
        }

        return Arrays.compareUnsigned(thisArray, otherArray);
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

    @Override
    public String toString() {
        return ByteUtil.toHexString(value, 32);
    }
}
