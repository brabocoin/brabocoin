package org.brabocoin.brabocoin.model;

import com.google.protobuf.ByteString;
import net.badata.protobuf.converter.annotation.ProtoClass;
import net.badata.protobuf.converter.annotation.ProtoField;
import org.brabocoin.brabocoin.proto.model.BrabocoinProtos;

@ProtoClass(BrabocoinProtos.Hash.class)
public class Hash {

    /**
     * Value of the hash.
     */
    @ProtoField
    private final ByteString value;

    /**
     * Creates a new Hash.
     * @param value The value of the hash.
     */
    public Hash(ByteString value) {
        this.value = value;
    }

    public ByteString getValue() {
        return value;
    }

    @ProtoClass(BrabocoinProtos.Hash.class)
    public static class Builder {
        @ProtoField
        private ByteString value;

        public Builder setValue(ByteString value) {
            this.value = value;
            return this;
        }

        public Hash createHash() {
            return new Hash(value);
        }
    }
}
