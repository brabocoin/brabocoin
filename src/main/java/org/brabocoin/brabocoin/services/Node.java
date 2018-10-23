package org.brabocoin.brabocoin.services;

import com.google.protobuf.Empty;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.MessageOrBuilder;
import com.google.protobuf.util.JsonFormat;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.stub.StreamObserver;
import org.brabocoin.brabocoin.model.Block;
import org.brabocoin.brabocoin.model.Hash;
import org.brabocoin.brabocoin.model.Transaction;
import org.brabocoin.brabocoin.model.messages.BlockHeight;
import org.brabocoin.brabocoin.model.messages.ChainCompatibility;
import org.brabocoin.brabocoin.model.messages.HandshakeResponse;
import org.brabocoin.brabocoin.node.NodeEnvironment;
import org.brabocoin.brabocoin.node.Peer;
import org.brabocoin.brabocoin.proto.model.BrabocoinProtos;
import org.brabocoin.brabocoin.proto.services.NodeGrpc;
import org.brabocoin.brabocoin.util.ProtoConverter;

import java.io.IOException;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * A full node on the Brabocoin network.
 */
public class Node {
    private final static Logger LOGGER = Logger.getLogger(NodeService.class.getName());

    /**
     * Service parameters
     */
    private final Server server;
    NodeEnvironment environment;

    public Node(final int listenPort, NodeEnvironment environment) {
        this.server = ServerBuilder.forPort(listenPort)
                .addService(new NodeService()).build();
        this.environment = environment;
    }

    public void start() throws IOException {
        LOGGER.info("Starting node.");
        environment.setup();
        server.start();

        Runtime.getRuntime().addShutdownHook(new Thread(Node.this::stop));
    }

    public void stop() {
        LOGGER.info("Stopping node.");
        if (server != null) {
            server.shutdown();
        }

        for (Peer p : environment.getPeers()) {
            LOGGER.log(Level.FINEST, "Stopping peer: {0}", p);
            p.stop();
        }
    }

    private StreamObserver<Empty> emptyLogResponseObserver = new StreamObserver<Empty>() {
        @Override
        public void onNext(Empty value) {
        }

        @Override
        public void onError(Throwable t) {
            LOGGER.warning("Propagation to peer failed.");
        }

        @Override
        public void onCompleted() {
            LOGGER.fine("Message propagation finished to peer.");
        }
    };

    public void blockUntilShutdown() throws InterruptedException {
        if (server != null) {
            server.awaitTermination();
        }
    }

    private void logIncomingCall(String call, MessageOrBuilder receivedMessage) {
        logIncomingCall(call, receivedMessage, Level.INFO);
    }

    private void logIncomingCall(String call, MessageOrBuilder receivedMessage, Level receivedLogLevel) {
        LOGGER.log(receivedLogLevel, "Received '{0}' call.", call);
        try {
            LOGGER.log(Level.FINEST, "Received data: {0}", JsonFormat.printer().print(receivedMessage));
        } catch (InvalidProtocolBufferException e) {
            LOGGER.log(Level.WARNING, "Could not log the JSON format of the response message.");
        }
    }

    private void logOutgoingResponse(MessageOrBuilder responseMessage) {
        try {
            LOGGER.log(Level.FINEST, "Responding with data: {0}", JsonFormat.printer().print(responseMessage));
        } catch (InvalidProtocolBufferException e) {
            LOGGER.log(Level.WARNING, "Could not log the JSON format of the response message.");
        }
    }

    private class NodeService extends NodeGrpc.NodeImplBase {
        @Override
        public void handshake(Empty request, StreamObserver<BrabocoinProtos.HandshakeResponse> responseObserver) {
            logIncomingCall("handshake", request);

            HandshakeResponse response = new HandshakeResponse(environment.getPeers().stream().map(Peer::toSocketString).collect(Collectors.toList()));
            BrabocoinProtos.HandshakeResponse protoResponse = ProtoConverter.toProto(response, BrabocoinProtos.HandshakeResponse.class);

            logOutgoingResponse(protoResponse);

            responseObserver.onNext(protoResponse);
            responseObserver.onCompleted();
            // TODO: logging
        }

        @Override
        public void announceBlock(BrabocoinProtos.Hash request, StreamObserver<Empty> responseObserver) {
            logIncomingCall("sendBlock", request);

            Hash hash = ProtoConverter.toDomain(request, Hash.Builder.class);
            environment.onReceiveBlockHash(hash);
            responseObserver.onCompleted();

            environment.propagateMessage((Peer p) -> p.getAsyncStub().sendBlock(request, emptyLogResponseObserver));
        }

        @Override
        public void announceTransaction(BrabocoinProtos.Hash request, StreamObserver<Empty> responseObserver) {
            logIncomingCall("sendTransaction", request);

            Hash hash = ProtoConverter.toDomain(request, Hash.Builder.class);
            environment.onReceiveTransaction(hash);
            responseObserver.onCompleted();

            environment.propagateMessage((Peer p) -> p.getAsyncStub().sendTransaction(request, emptyLogResponseObserver));
        }

        @Override
        public StreamObserver<BrabocoinProtos.Hash> getBlocks(StreamObserver<BrabocoinProtos.Block> responseObserver) {
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

                    BrabocoinProtos.Block protoBlock = ProtoConverter.toProto(block, BrabocoinProtos.Block.class);

                    logOutgoingResponse(protoBlock);

                    responseObserver.onNext(protoBlock);
                }

                @Override
                public void onError(Throwable t) {
                    LOGGER.log(Level.WARNING, "Error occurred during getBlocks stream: {0}", t.getMessage());
                }

                @Override
                public void onCompleted() {
                    responseObserver.onCompleted();

                    LOGGER.fine("GetBlocks completed.");
                }
            };
        }

        @Override
        public StreamObserver<BrabocoinProtos.Hash> getTransactions(StreamObserver<BrabocoinProtos.Transaction> responseObserver) {
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

                    BrabocoinProtos.Transaction protoTransaction = ProtoConverter.toProto(transaction, BrabocoinProtos.Transaction.class);

                    logOutgoingResponse(protoTransaction);

                    responseObserver.onNext(protoTransaction);
                }

                @Override
                public void onError(Throwable t) {
                    LOGGER.log(Level.WARNING, "Error occurred during getTransactions stream: {0}", t.getMessage());
                }

                @Override
                public void onCompleted() {
                    responseObserver.onCompleted();

                    LOGGER.fine("GetBlocks completed.");
                }
            };
        }

        @Override
        public void seekTransactionPool(Empty request, StreamObserver<BrabocoinProtos.Hash> responseObserver) {
            logIncomingCall("seekTransactionPool", request);
            Iterator<Hash> transactionIterator = environment.getTransactionIterator();
            for (Iterator<Hash> it = transactionIterator; it.hasNext(); ) {
                Hash h = it.next();

                BrabocoinProtos.Hash protoHash = ProtoConverter.toProto(h, BrabocoinProtos.Hash.class);
                logOutgoingResponse(protoHash);
                responseObserver.onNext(protoHash);
            }

            responseObserver.onCompleted();
        }

        @Override
        public void discoverTopBlockHeight(Empty request, StreamObserver<BrabocoinProtos.BlockHeight> responseObserver) {
            logIncomingCall("discoverTopBlockHeight", request);
            BlockHeight blockHeight = new BlockHeight(environment.getTopBlockHeight());
            BrabocoinProtos.BlockHeight protoBlockHeight = ProtoConverter.toProto(blockHeight, BrabocoinProtos.BlockHeight.class);

            logOutgoingResponse(protoBlockHeight);
            responseObserver.onNext(protoBlockHeight);
            responseObserver.onCompleted();
        }

        @Override
        public void checkChainCompatible(BrabocoinProtos.Hash request, StreamObserver<BrabocoinProtos.ChainCompatibility> responseObserver) {
            logIncomingCall("checkChainCompatible", request);
            Hash hash = ProtoConverter.toDomain(request, Hash.Builder.class);
            ChainCompatibility compatibility = new ChainCompatibility(environment.isChainCompatible(hash));
            BrabocoinProtos.ChainCompatibility protoChainCompatiblity = ProtoConverter.toProto(compatibility, BrabocoinProtos.ChainCompatibility.class);

            logOutgoingResponse(protoChainCompatiblity);
            responseObserver.onNext(protoChainCompatiblity);
            responseObserver.onCompleted();
        }

        @Override
        public void seekBlockchain(BrabocoinProtos.Hash request, StreamObserver<BrabocoinProtos.Hash> responseObserver) {
            logIncomingCall("seekBlockchain", request);
            Hash hash = ProtoConverter.toDomain(request, Hash.Builder.class);
            Iterator<Hash> blockIterator = environment.getBlocksAbove(hash);
            for (Iterator<Hash> it = blockIterator; it.hasNext(); ) {
                Hash h = it.next();

                BrabocoinProtos.Hash protoHash = ProtoConverter.toProto(h, BrabocoinProtos.Hash.class);

                logOutgoingResponse(protoHash);
                responseObserver.onNext(protoHash);
            }

            responseObserver.onCompleted();
        }
    }
}
