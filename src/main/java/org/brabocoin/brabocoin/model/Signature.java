package org.brabocoin.brabocoin.model;

import net.badata.protobuf.converter.annotation.ProtoClass;
import org.brabocoin.brabocoin.model.proto.ProtoBuilder;
import org.brabocoin.brabocoin.model.proto.ProtoModel;
import org.brabocoin.brabocoin.proto.model.BrabocoinProtos;

@ProtoClass(BrabocoinProtos.Signature.class)
public class Signature implements ProtoModel<Signature> {

    @Override
    public Class<? extends ProtoBuilder> getBuilder() {
        return Builder.class;
    }

    @ProtoClass(BrabocoinProtos.Signature.class)
    public static class Builder implements ProtoBuilder<Signature> {

        @Override
        public Signature build() {
            return new Signature();
        }
    }
}
