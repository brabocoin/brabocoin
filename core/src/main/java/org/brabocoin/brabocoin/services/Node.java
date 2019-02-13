package org.brabocoin.brabocoin.services;

import com.dosse.upnp.UPnP;
import com.google.protobuf.Empty;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import com.google.protobuf.MessageOrBuilder;
import com.google.protobuf.util.JsonFormat;
import io.grpc.ForwardingServerCall;
import io.grpc.ForwardingServerCallListener;
import io.grpc.Grpc;
import io.grpc.Metadata;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import io.grpc.ServerInterceptors;
import io.grpc.stub.StreamObserver;
import org.brabocoin.brabocoin.exceptions.MalformedSocketException;
import org.brabocoin.brabocoin.model.Block;
import org.brabocoin.brabocoin.model.Hash;
import org.brabocoin.brabocoin.model.Transaction;
import org.brabocoin.brabocoin.model.messages.BlockHeight;
import org.brabocoin.brabocoin.model.messages.ChainCompatibility;
import org.brabocoin.brabocoin.model.messages.HandshakeResponse;
import org.brabocoin.brabocoin.node.MessageArtifact;
import org.brabocoin.brabocoin.node.NetworkMessage;
import org.brabocoin.brabocoin.node.NetworkMessageListener;
import org.brabocoin.brabocoin.node.NodeEnvironment;
import org.brabocoin.brabocoin.node.Peer;
import org.brabocoin.brabocoin.proto.model.BrabocoinProtos;
import org.brabocoin.brabocoin.proto.services.NodeGrpc;
import org.brabocoin.brabocoin.util.ProtoConverter;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * A full node on the Brabocoin network.
 */
public class Node {

    private final static Logger LOGGER = Logger.getLogger(Node.class.getName());
    private final List<NetworkMessageListener> networkMessageListeners = new ArrayList<>();
    private final AtomicReference<ServerCall<?, ?>> serverCallCapture =
        new AtomicReference<>();

    private final int servicePort;
    /**
     * The network id of this node
     */
    private final int networkId;

    /**
     * Service parameters
     */
    private final Server server;
    @NotNull private NodeEnvironment environment;

    /**
     * Captures the request attributes. Useful for testing ServerCalls.
     * {@link ServerCall#getAttributes()}
     */
    private ServerInterceptor recordServerCallInterceptor() {
        return new ServerInterceptor() {
            @Override
            public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
                ServerCall<ReqT, RespT> call,
                Metadata requestHeaders,
                ServerCallHandler<ReqT, RespT> next) {
                serverCallCapture.set(call);
                InetSocketAddress clientAddress = (InetSocketAddress)call.getAttributes()
                    .get(Grpc.TRANSPORT_ATTR_REMOTE_ADDR);
                Peer peer = null;
                try {
                    peer = new Peer(clientAddress);
                }
                catch (MalformedSocketException e) {
                    // ignore
                }
                NetworkMessage networkMessage = new NetworkMessage(peer);
                networkMessage.setMethodDescriptor(call.getMethodDescriptor());
                return createCallListener(next.startCall(
                    new ForwardingServerCall.SimpleForwardingServerCall<ReqT, RespT>(call) {
                        @Override
                        public void sendMessage(RespT message) {
                            super.sendMessage(message);
                            networkMessage.addResponseMessage(
                                new MessageArtifact((Message)message)
                            );
                            networkMessageListeners.forEach(l ->
                                l.onIncomingMessage(
                                    networkMessage,
                                    true
                                ));
                        }
                    }, requestHeaders), networkMessage);
            }
        };
    }


    private <ReqT> ForwardingServerCallListener.SimpleForwardingServerCallListener<ReqT> createCallListener(
        ServerCall.Listener<ReqT> listener,
        NetworkMessage networkMessage) {
        return new ForwardingServerCallListener.SimpleForwardingServerCallListener<ReqT>(listener) {
            @Override
            public void onComplete() {
                super.onComplete();
            }

            @Override
            public void onMessage(ReqT message) {
                super.onMessage(message);
                networkMessage.addRequestMessage(
                    new MessageArtifact((Message)message)
                );
                networkMessageListeners.forEach(l ->
                    l.onIncomingMessage(
                        networkMessage,
                        false
                    ));
            }
        };
    }

    public Node(NodeEnvironment environment, int servicePort, int networkId) {
        this.server = ServerBuilder.forPort(servicePort)
            .executor(Executors.newFixedThreadPool(4))
            .addService(
                ServerInterceptors.intercept(
                    new NodeService(),
                    recordServerCallInterceptor()
                )
            )
            .build();
        this.environment = environment;
        this.servicePort = servicePort;
        this.networkId = networkId;

        networkMessageListeners.add(this.environment);
    }

    public void start() throws IOException {
        LOGGER.info("Starting node.");
        attemptUPnP();
        environment.setup();
        server.start();

        Runtime.getRuntime().addShutdownHook(new Thread(Node.this::stop));
    }

    private void attemptUPnP() {
        if (!UPnP.isUPnPAvailable()) {
            LOGGER.log(Level.WARNING, "UPnP not available in current network, will not attempt to open port.");
            return;
        }

        try {
            UPnP.openPortTCP(servicePort);
            LOGGER.log(Level.INFO, MessageFormat.format("UPnP port ({0}) opened.", servicePort));
        }
        catch (Exception e) {
            LOGGER.log(Level.WARNING, MessageFormat.format("UPnP port ({0}) opening did not succeed.", servicePort));
        }
    }

    public void stop() {
        LOGGER.info("Stopping node.");
        if (server != null) {
            server.shutdown();
        }

        environment.stop();
    }

    public void stopAndBlock() throws InterruptedException {
        stop();
        blockUntilShutdown();
    }

    public void blockUntilShutdown() throws InterruptedException {
        if (server != null) {
            server.awaitTermination();
        }
    }

    private void logIncomingCall(String call, MessageOrBuilder receivedMessage) {
        logIncomingCall(call, receivedMessage, Level.INFO);
    }

    private void logIncomingCall(String call, MessageOrBuilder receivedMessage,
                                 Level receivedLogLevel) {
        LOGGER.log(receivedLogLevel, "Received {0} call.", call);
        LOGGER.log(Level.FINEST, () -> {
            try {
                return MessageFormat.format(
                    "Received data: {0}",
                    JsonFormat.printer().print(receivedMessage)
                );
            }
            catch (InvalidProtocolBufferException e) {
                LOGGER.log(
                    Level.WARNING,
                    "Could not log the JSON format of the response message.",
                    e
                );
            }

            return "";
        });
    }

    private void logOutgoingResponse(MessageOrBuilder responseMessage) {
        LOGGER.log(Level.FINEST, () -> {
            try {
                return MessageFormat.format(
                    "Responding with data: {0}",
                    JsonFormat.printer().print(responseMessage)
                );
            }
            catch (InvalidProtocolBufferException e) {
                LOGGER.log(
                    Level.WARNING,
                    "Could not log the JSON format of the response message.",
                    e
                );
            }
            return "";
        });
    }

    private class NodeService extends NodeGrpc.NodeImplBase {

        @Override
        public void handshake(BrabocoinProtos.HandshakeRequest request,
                              StreamObserver<BrabocoinProtos.HandshakeResponse> responseObserver) {
            logIncomingCall("handshake", request);

            HandshakeResponse response = new HandshakeResponse(
                environment.getPeers()
                    .stream()
                    .map(Peer::toSocketString)
                    .collect(Collectors.toList()),
                networkId
            );
            BrabocoinProtos.HandshakeResponse protoResponse = ProtoConverter.toProto(
                response,
                BrabocoinProtos.HandshakeResponse.class
            );
            logOutgoingResponse(protoResponse);

            responseObserver.onNext(protoResponse);
            responseObserver.onCompleted();

            if (networkId != request.getNetworkId()) {
                return;
            }

            // Add the client as a valid peer.
            InetSocketAddress clientAddress = (InetSocketAddress)serverCallCapture.get()
                .getAttributes()
                .get(Grpc.TRANSPORT_ATTR_REMOTE_ADDR);
            if (clientAddress != null) {
                environment.addClientPeer(clientAddress.getAddress(), request.getServicePort());
            }
        }

        @Override
        public void announceBlock(BrabocoinProtos.Hash request,
                                  StreamObserver<Empty> responseObserver) {
            logIncomingCall("announceBlock", request);

            Hash hash = ProtoConverter.toDomain(request, Hash.Builder.class);
            responseObserver.onNext(Empty.newBuilder().build());
            responseObserver.onCompleted();

            InetSocketAddress clientAddress = (InetSocketAddress)serverCallCapture.get()
                .getAttributes()
                .get(Grpc.TRANSPORT_ATTR_REMOTE_ADDR);
            if (clientAddress != null) {
                List<Peer> peers = environment.findClientPeers(clientAddress.getAddress());
                environment.onReceiveBlockHash(hash, peers);
            }
            else {
                LOGGER.log(
                    Level.WARNING,
                    "Could not find the client peer announcing the block hash, ignoring."
                );
            }
        }

        @Override
        public void announceTransaction(BrabocoinProtos.Hash request,
                                        StreamObserver<Empty> responseObserver) {
            logIncomingCall("announceTransaction", request);

            Hash hash = ProtoConverter.toDomain(request, Hash.Builder.class);
            responseObserver.onNext(Empty.newBuilder().build());
            responseObserver.onCompleted();

            InetSocketAddress clientAddress = (InetSocketAddress)serverCallCapture.get()
                .getAttributes()
                .get(Grpc.TRANSPORT_ATTR_REMOTE_ADDR);
            if (clientAddress != null) {
                List<Peer> peers = environment.findClientPeers(clientAddress.getAddress());
                environment.onReceiveTransactionHash(hash, peers);
            }
            else {
                LOGGER.log(
                    Level.WARNING,
                    "Could not find the client peer announcing the block hash, ignoring."
                );
            }
        }

        @Override
        public StreamObserver<BrabocoinProtos.Hash> getBlocks(
            StreamObserver<BrabocoinProtos.Block> responseObserver) {
            LOGGER.log(Level.INFO, "getBlocks message received.");
            return new StreamObserver<BrabocoinProtos.Hash>() {
                @Override
                public void onNext(BrabocoinProtos.Hash value) {
                    logIncomingCall("getBlocks.onNext", value, Level.FINE);
                    Hash hash = ProtoConverter.toDomain(value, Hash.Builder.class);
                    Block block = environment.getBlock(hash);
                    if (block == null) {
                        return;
                    }

                    BrabocoinProtos.Block protoBlock = ProtoConverter.toProto(
                        block,
                        BrabocoinProtos.Block.class
                    );

                    logOutgoingResponse(protoBlock);

                    responseObserver.onNext(protoBlock);
                }

                @Override
                public void onError(Throwable t) {
                    LOGGER.log(
                        Level.WARNING,
                        "Error occurred during getBlocks stream: {0}",
                        t.getMessage()
                    );
                }

                @Override
                public void onCompleted() {
                    responseObserver.onCompleted();

                    LOGGER.fine("GetBlocks completed.");
                }
            };
        }

        @Override
        public StreamObserver<BrabocoinProtos.Hash> getTransactions(
            StreamObserver<BrabocoinProtos.Transaction> responseObserver) {
            LOGGER.log(Level.INFO, "getTransactions message received.");
            return new StreamObserver<BrabocoinProtos.Hash>() {
                @Override
                public void onNext(BrabocoinProtos.Hash value) {
                    logIncomingCall("getTransactions.onNext", value, Level.FINE);
                    Hash hash = ProtoConverter.toDomain(value, Hash.Builder.class);
                    Transaction transaction = environment.getTransaction(hash);
                    if (transaction == null) {
                        return;
                    }

                    BrabocoinProtos.Transaction protoTransaction = ProtoConverter.toProto(
                        transaction,
                        BrabocoinProtos.Transaction.class
                    );

                    logOutgoingResponse(protoTransaction);

                    responseObserver.onNext(protoTransaction);
                }

                @Override
                public void onError(Throwable t) {
                    LOGGER.log(
                        Level.WARNING,
                        "Error occurred during getTransactions stream: {0}",
                        t.getMessage()
                    );
                }

                @Override
                public void onCompleted() {
                    responseObserver.onCompleted();

                    LOGGER.fine("GetBlocks completed.");
                }
            };
        }

        @Override
        public void seekTransactionPool(Empty request,
                                        StreamObserver<BrabocoinProtos.Hash> responseObserver) {
            logIncomingCall("seekTransactionPool", request);
            Set<Hash> transactionHashSet = environment.getTransactionHashSet();
            for (Hash h : transactionHashSet) {
                BrabocoinProtos.Hash protoHash = ProtoConverter.toProto(
                    h,
                    BrabocoinProtos.Hash.class
                );
                logOutgoingResponse(protoHash);
                responseObserver.onNext(protoHash);
            }

            responseObserver.onCompleted();
        }

        @Override
        public void discoverTopBlockHeight(Empty request,
                                           StreamObserver<BrabocoinProtos.BlockHeight> responseObserver) {
            logIncomingCall("discoverTopBlockHeight", request);
            BlockHeight blockHeight = new BlockHeight(environment.getTopBlockHeight());
            BrabocoinProtos.BlockHeight protoBlockHeight = ProtoConverter.toProto(
                blockHeight,
                BrabocoinProtos.BlockHeight.class
            );

            logOutgoingResponse(protoBlockHeight);
            responseObserver.onNext(protoBlockHeight);
            responseObserver.onCompleted();
        }

        @Override
        public void checkChainCompatible(BrabocoinProtos.Hash request,
                                         StreamObserver<BrabocoinProtos.ChainCompatibility> responseObserver) {
            logIncomingCall("checkChainCompatible", request);
            Hash hash = ProtoConverter.toDomain(request, Hash.Builder.class);
            ChainCompatibility compatibility = new ChainCompatibility(
                environment.isChainCompatible(hash)
            );
            BrabocoinProtos.ChainCompatibility protoChainCompatibility = ProtoConverter.toProto(
                compatibility,
                BrabocoinProtos.ChainCompatibility.class
            );

            logOutgoingResponse(protoChainCompatibility);
            responseObserver.onNext(protoChainCompatibility);
            responseObserver.onCompleted();
        }

        @Override
        public void seekBlockchain(BrabocoinProtos.Hash request,
                                   StreamObserver<BrabocoinProtos.Hash> responseObserver) {
            logIncomingCall("seekBlockchain", request);
            Hash hash = ProtoConverter.toDomain(request, Hash.Builder.class);
            List<Hash> hashList = environment.getBlocksAbove(hash);
            for (Hash h : hashList) {
                BrabocoinProtos.Hash protoHash = ProtoConverter.toProto(
                    h,
                    BrabocoinProtos.Hash.class
                );

                logOutgoingResponse(protoHash);
                responseObserver.onNext(protoHash);
            }

            responseObserver.onCompleted();
        }
    }

    public @NotNull NodeEnvironment getEnvironment() {
        return environment;
    }

    public void addNetworkMessageListener(NetworkMessageListener listener) {
        networkMessageListeners.add(listener);
    }

    public void removeNetworkMessageListeners(NetworkMessageListener listener) {
        networkMessageListeners.remove(listener);
    }
}
