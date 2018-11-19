package org.brabocoin.brabocoin.model.messages;

import net.badata.protobuf.converter.annotation.ProtoClass;
import net.badata.protobuf.converter.annotation.ProtoField;
import org.brabocoin.brabocoin.model.proto.ProtoBuilder;
import org.brabocoin.brabocoin.model.proto.ProtoModel;
import org.brabocoin.brabocoin.proto.model.BrabocoinProtos;

@ProtoClass(BrabocoinProtos.BlockHeight.class)
public class BlockHeight implements ProtoModel<BlockHeight> {

    @ProtoField
    private int height;

    public BlockHeight(int height) {
        this.height = height;
    }

    @Override
    public Class<? extends ProtoBuilder> getBuilder() {
        return Builder.class;
    }

    public int getHeight() {
        return height;
    }

    @ProtoClass(BrabocoinProtos.BlockHeight.class)
    public static class Builder implements ProtoBuilder<BlockHeight> {

        @ProtoField
        private int height;

        public Builder setHeight(int height) {
            this.height = height;
            return this;
        }

        @Override
        public BlockHeight build() {
            return new BlockHeight(height);
        }
    }
}
