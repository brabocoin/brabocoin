package org.brabocoin.brabocoin.node;

import com.google.protobuf.Message;
import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.ClientCall;
import io.grpc.ClientInterceptor;
import io.grpc.ForwardingClientCall;
import io.grpc.ForwardingClientCallListener;
import io.grpc.Metadata;
import io.grpc.MethodDescriptor;

import java.util.ArrayList;
import java.util.List;

public class PeerMessageInterceptor {

    private final List<NetworkMessageListener> networkMessageListeners = new ArrayList<>();
    private final Peer peer;

    public PeerMessageInterceptor(Peer peer) {
        this.peer = peer;
    }

    public ClientInterceptor createClientCallInterceptor() {
        return new ClientInterceptor() {
            @Override
            public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(
                MethodDescriptor<ReqT, RespT> method, CallOptions callOptions, Channel next) {
                NetworkMessage networkMessage = new NetworkMessage(peer);
                networkMessage.setMethodDescriptor(method);
                return new ForwardingClientCall.SimpleForwardingClientCall<ReqT, RespT>(
                    next.newCall(method, callOptions)
                ) {
                    @Override
                    public void start(Listener<RespT> responseListener, Metadata headers) {
                        super.start(createCallListener(responseListener, networkMessage), headers);
                    }

                    @Override
                    public void sendMessage(ReqT message) {
                        super.sendMessage(message);
                        networkMessage.setRequestTime();
                        networkMessage.setRequestMessage((Message)message);
                        networkMessageListeners.forEach(
                            l -> l.onOutgoingMessage(
                                networkMessage,
                                false
                            ));
                    }
                };
            }
        };
    }

    private <RespT> ForwardingClientCallListener.SimpleForwardingClientCallListener<RespT> createCallListener(
        ClientCall.Listener<RespT> forwarder, NetworkMessage networkMessage) {
        return new ForwardingClientCallListener.SimpleForwardingClientCallListener<RespT>(forwarder) {
            @Override
            public void onMessage(RespT message) {
                super.onMessage(message);
                networkMessage.setResponseTime();
                networkMessage.setResponseMessage((Message)message);
                networkMessageListeners.forEach(
                    l -> l.onOutgoingMessage(
                        networkMessage,
                        true
                    ));
            }
        };
    }

    public void addNetworkMessageListener(NetworkMessageListener listener) {
        networkMessageListeners.add(listener);
    }

    public void removeNetworkMessageListeners(NetworkMessageListener listener) {
        networkMessageListeners.remove(listener);
    }
}
