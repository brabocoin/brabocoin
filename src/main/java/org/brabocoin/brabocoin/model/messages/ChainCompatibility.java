package org.brabocoin.brabocoin.model.messages;

import net.badata.protobuf.converter.annotation.ProtoClass;
import net.badata.protobuf.converter.annotation.ProtoField;
import org.brabocoin.brabocoin.proto.model.BrabocoinProtos;

@ProtoClass(BrabocoinProtos.ChainCompatibility.class)
public class ChainCompatibility {
    @ProtoField
    private boolean compatible;

    public ChainCompatibility(boolean compatible) {
        this.compatible = compatible;
    }

    public static class Builder {
        private boolean compatible;

        public Builder setCompatible(boolean compatible) {
            this.compatible = compatible;
            return this;
        }

        public ChainCompatibility createChainCompatibility() {
            return new ChainCompatibility(compatible);
        }
    }
}
