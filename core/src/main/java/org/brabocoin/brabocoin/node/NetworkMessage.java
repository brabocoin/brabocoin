package org.brabocoin.brabocoin.node;

import io.grpc.MethodDescriptor;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class NetworkMessage implements Comparable<NetworkMessage> {
    private final boolean incoming;
    private final Peer peer;
    private List<MessageArtifact> requestMessages = new ArrayList<>();
    private List<MessageArtifact> responseMessages = new ArrayList<>();
    private MethodDescriptor<?, ?> methodDescriptor;
    private long accumulatedSize = 0;

    public NetworkMessage(Peer peer, boolean incoming)
    {
        this.peer = peer;
        this.incoming = incoming;
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
        return Collections.unmodifiableList(requestMessages);
    }

    public List<MessageArtifact> getResponseMessages() {
        return Collections.unmodifiableList(responseMessages);
    }

    public void addRequestMessage(MessageArtifact artifact) {
        requestMessages.add(artifact);
        accumulatedSize += artifact.getMessage().getSerializedSize();
    }

    public void addResponseMessage(MessageArtifact artifact) {
        responseMessages.add(artifact);
        accumulatedSize += artifact.getMessage().getSerializedSize();
    }

    public boolean isIncoming() {
        return incoming;
    }

    public long getAccumulatedSize() {
        return accumulatedSize;
    }
}
