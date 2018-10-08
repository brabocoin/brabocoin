package org.brabocoin.brabocoin.model.messages;

import net.badata.protobuf.converter.annotation.ProtoClass;
import net.badata.protobuf.converter.annotation.ProtoField;
import org.brabocoin.brabocoin.proto.model.BrabocoinProtos;

@ProtoClass(BrabocoinProtos.BlockHeight.class)
public class BlockHeight {
    @ProtoField
    private long height;

    public BlockHeight(long height) {
        this.height = height;
    }

    public static class Builder {
        private long height;

        public Builder setHeight(long height) {
            this.height = height;
            return this;
        }

        public BlockHeight createBlockHeight() {
            return new BlockHeight(height);
        }
    }
}
