package org.brabocoin.brabocoin.model.messages;

import net.badata.protobuf.converter.annotation.ProtoClass;
import net.badata.protobuf.converter.annotation.ProtoField;
import org.brabocoin.brabocoin.proto.model.BrabocoinProtos;

import java.util.List;

@ProtoClass(BrabocoinProtos.HandshakeResponse.class)
public class HandshakeResponse {
    @ProtoField
    private final List<String> peers;

    public HandshakeResponse(List<String> peers) {
        this.peers = peers;
    }

    public List<String> getPeers() {
        return peers;
    }

    @ProtoClass(BrabocoinProtos.HandshakeResponse.class)
    public static class Builder {
        @ProtoField
        private List<String> peers;

        public Builder setPeers(List<String> peers) {
            this.peers = peers;
            return this;
        }

        public HandshakeResponse createHandshakeResponse() {
            return new HandshakeResponse(peers);
        }
    }
}
