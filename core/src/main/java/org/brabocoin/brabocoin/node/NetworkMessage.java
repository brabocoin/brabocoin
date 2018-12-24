package org.brabocoin.brabocoin.node;

import io.grpc.MethodDescriptor;

public class NetworkMessage {

    private Peer peer;
    private String data;
    private MethodDescriptor<?, ?> methodDescriptor;

    public NetworkMessage(Peer peer, String data, MethodDescriptor<?, ?> methodDescriptor) {
        this.peer = peer;
        this.data = data;
        this.methodDescriptor = methodDescriptor;
    }

    public Peer getPeer() {
        return peer;
    }

    public String getData() {
        return data;
    }

    public MethodDescriptor<?, ?> getMethodDescriptor() {
        return methodDescriptor;
    }
}
