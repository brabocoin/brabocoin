package org.brabocoin.brabocoin.node;

import com.google.protobuf.Message;
import io.grpc.MethodDescriptor;
import org.jetbrains.annotations.NotNull;

import java.time.LocalDateTime;

public class NetworkMessage implements Comparable<NetworkMessage> {

    private final Peer peer;
    private Message requestMessage;
    private Message responseMessage;
    private MethodDescriptor<?, ?> methodDescriptor;
    private LocalDateTime requestTime;
    private LocalDateTime responseTime;

    public NetworkMessage(Peer peer) {
        this.peer = peer;
    }

    public Peer getPeer() {
        return peer;
    }

    public Message getRequestMessage() {
        return requestMessage;
    }

    public MethodDescriptor<?, ?> getMethodDescriptor() {
        return methodDescriptor;
    }

    public LocalDateTime getRequestTime() {
        return requestTime;
    }

    @Override
    public int compareTo(@NotNull NetworkMessage o) {
        return o.getRequestTime().compareTo(this.getRequestTime());
    }

    public void setRequestMessage(Message requestMessage) {
        this.requestMessage = requestMessage;
    }

    public void setMethodDescriptor(MethodDescriptor<?, ?> methodDescriptor) {
        this.methodDescriptor = methodDescriptor;
    }

    public void setRequestTime() {
        this.requestTime = LocalDateTime.now();
    }

    public void setResponseTime() {
        this.responseTime = LocalDateTime.now();
    }

    public LocalDateTime getResponseTime() {
        return responseTime;
    }

    public Message getResponseMessage() {
        return responseMessage;
    }

    public void setResponseMessage(Message responseMessage) {
        this.responseMessage = responseMessage;
    }
}
