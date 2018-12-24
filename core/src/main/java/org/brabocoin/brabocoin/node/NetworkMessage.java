package org.brabocoin.brabocoin.node;

import com.google.protobuf.Message;
import io.grpc.MethodDescriptor;
import org.jetbrains.annotations.NotNull;

import java.time.LocalDateTime;

public class NetworkMessage implements Comparable<NetworkMessage> {

    private final LocalDateTime time;
    private final Peer peer;
    private final Message message;
    private final MethodDescriptor<?, ?> methodDescriptor;

    public NetworkMessage(Peer peer, Message message, MethodDescriptor<?, ?> methodDescriptor) {
        this.peer = peer;
        this.message = message;
        this.methodDescriptor = methodDescriptor;
        this.time = LocalDateTime.now();
    }

    public Peer getPeer() {
        return peer;
    }

    public Message getMessage() {
        return message;
    }

    public MethodDescriptor<?, ?> getMethodDescriptor() {
        return methodDescriptor;
    }

    public LocalDateTime getTime() {
        return time;
    }

    @Override
    public int compareTo(@NotNull NetworkMessage o) {
        return o.getTime().compareTo(this.getTime());
    }
}
