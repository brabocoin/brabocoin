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

    @ProtoField
    private int networkId;

    public HandshakeRequest(int servicePort, int networkId) {
        this.servicePort = servicePort;
        this.networkId = networkId;
    }

    @Override
    public Class<? extends ProtoBuilder> getBuilder() {
        return Builder.class;
    }

    public int getServicePort() {
        return servicePort;
    }

    public int getNetworkId() {
        return networkId;
    }

    @ProtoClass(BrabocoinProtos.HandshakeRequest.class)
    public static class Builder implements ProtoBuilder<HandshakeRequest> {

        @ProtoField
        private int servicePort;

        @ProtoField
        private int networkId;

        public void setServicePort(int servicePort) {
            this.servicePort = servicePort;
        }

        public void setNetworkId(int networkId) {
            this.networkId = networkId;
        }

        @Override
        public HandshakeRequest build() {
            return new HandshakeRequest(servicePort, networkId);
        }
    }
}
