package org.brabocoin.brabocoin.node;

import io.grpc.MethodDescriptor;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class NetworkMessage implements Comparable<NetworkMessage> {

    private final Peer peer;
    private List<MessageArtifact> requestMessages = new ArrayList<>();
    private List<MessageArtifact> responseMessages = new ArrayList<>();
    private MethodDescriptor<?, ?> methodDescriptor;

    public NetworkMessage(Peer peer) {
        this.peer = peer;
    }

    public Peer getPeer() {
        return peer;
    }

    public MethodDescriptor<?, ?> getMethodDescriptor() {
        return methodDescriptor;
    }

    @Override
    public int compareTo(@NotNull NetworkMessage o) {
        if (o.getRequestMessages().size() <= 0 || this.getRequestMessages().size() <= 0) {
            return 0;
        }
        return o.getRequestMessages().get(0).compareTo(this.getRequestMessages().get(0));
    }

    public void setMethodDescriptor(MethodDescriptor<?, ?> methodDescriptor) {
        this.methodDescriptor = methodDescriptor;
    }

    public List<MessageArtifact> getRequestMessages() {
        return requestMessages;
    }

    public List<MessageArtifact> getResponseMessages() {
        return responseMessages;
    }

    public void addRequestMessage(MessageArtifact artifact) {
        requestMessages.add(artifact);
    }

    public void addResponseMessage(MessageArtifact artifact) {
        responseMessages.add(artifact);
    }
}
