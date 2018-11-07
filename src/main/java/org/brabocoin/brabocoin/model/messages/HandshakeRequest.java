package org.brabocoin.brabocoin.model.messages;

import net.badata.protobuf.converter.annotation.ProtoClass;
import net.badata.protobuf.converter.annotation.ProtoField;
import org.brabocoin.brabocoin.model.proto.ProtoBuilder;
import org.brabocoin.brabocoin.model.proto.ProtoModel;
import org.brabocoin.brabocoin.proto.model.BrabocoinProtos;

@ProtoClass(BrabocoinProtos.HandshakeRequest.class)
public class HandshakeRequest implements ProtoModel<HandshakeRequest> {

    @ProtoField
    private int servicePort;

    public HandshakeRequest(int servicePort) {
        this.servicePort = servicePort;
    }

    @Override
    public Class<? extends ProtoBuilder> getBuilder() {
        return Builder.class;
    }

    public int getServicePort() {
        return servicePort;
    }

    @ProtoClass(BrabocoinProtos.HandshakeRequest.class)
    public static class Builder implements ProtoBuilder<HandshakeRequest> {
        @ProtoField
        private int servicePort;

        public Builder setServicePort(int servicePort) {
            this.servicePort = servicePort;
            return this;
        }

        @Override
        public HandshakeRequest build() {
            return new HandshakeRequest(servicePort);
        }
    }
}
