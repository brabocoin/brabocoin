package org.brabocoin.brabocoin.model.messages;

import net.badata.protobuf.converter.annotation.ProtoClass;
import net.badata.protobuf.converter.annotation.ProtoField;
import org.brabocoin.brabocoin.model.proto.ProtoBuilder;
import org.brabocoin.brabocoin.model.proto.ProtoModel;
import org.brabocoin.brabocoin.proto.model.BrabocoinProtos;

import java.util.List;

@ProtoClass(BrabocoinProtos.HandshakeResponse.class)
public class HandshakeResponse implements ProtoModel<HandshakeResponse> {

    @ProtoField
    private final List<String> peers;

    @ProtoField
    private int networkId;

    public HandshakeResponse(List<String> peers, int networkId) {
        this.peers = peers;
        this.networkId = networkId;
    }

    public List<String> getPeers() {
        return peers;
    }

    @Override
    public Class<? extends ProtoBuilder> getBuilder() {
        return Builder.class;
    }

    public int getNetworkId() {
        return networkId;
    }

    @ProtoClass(BrabocoinProtos.HandshakeResponse.class)
    public static class Builder implements ProtoBuilder<HandshakeResponse> {

        @ProtoField
        private List<String> peers;

        @ProtoField
        private int networkId;

        public void setPeers(List<String> peers) {
            this.peers = peers;
        }

        public void setNetworkId(int networkId) {
            this.networkId = networkId;
        }

        @Override
        public HandshakeResponse build() {
            return new HandshakeResponse(peers, networkId);
        }
    }
}
