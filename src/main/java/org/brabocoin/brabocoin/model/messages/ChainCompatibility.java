package org.brabocoin.brabocoin.model.messages;

import net.badata.protobuf.converter.annotation.ProtoClass;
import net.badata.protobuf.converter.annotation.ProtoField;
import org.brabocoin.brabocoin.model.proto.ProtoBuilder;
import org.brabocoin.brabocoin.model.proto.ProtoModel;
import org.brabocoin.brabocoin.proto.model.BrabocoinProtos;

@ProtoClass(BrabocoinProtos.ChainCompatibility.class)
public class ChainCompatibility implements ProtoModel<ChainCompatibility> {

    @ProtoField
    private boolean compatible;

    public ChainCompatibility(boolean compatible) {
        this.compatible = compatible;
    }

    @Override
    public Class<? extends ProtoBuilder> getBuilder() {
        return Builder.class;
    }

    @ProtoClass(BrabocoinProtos.ChainCompatibility.class)
    public static class Builder implements ProtoBuilder<ChainCompatibility> {

        @ProtoField
        private boolean compatible;

        public Builder setCompatible(boolean compatible) {
            this.compatible = compatible;
            return this;
        }

        @Override
        public ChainCompatibility build() {
            return new ChainCompatibility(compatible);
        }
    }
}
