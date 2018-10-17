package org.brabocoin.brabocoin.services;

import com.google.protobuf.Empty;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.stub.StreamObserver;
import net.badata.protobuf.converter.Converter;
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

import java.io.IOException;
import java.util.Iterator;
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
    private final int listenPort;
    private final Server server;
    NodeEnvironment environment;

    public Node(final int listenPort, NodeEnvironment environment) {
        this.listenPort = listenPort;
        this.server = ServerBuilder.forPort(listenPort)
                .addService(new NodeService()).build();
        this.environment = environment;
    }

    void start() throws IOException {
        environment.setup();
        server.start();

        Runtime.getRuntime().addShutdownHook(new Thread(Node.this::stop));
    }

    void stop() {
        if (server != null) {
            server.shutdown();
        }

        for (Peer p : environment.getPeers()) {
            p.stop();
        }
    }

    public void blockUntilShutdown() throws InterruptedException {
        if (server != null) {
            server.awaitTermination();
        }
    }

    private class NodeService extends NodeGrpc.NodeImplBase {
        @Override
        public void handshake(Empty request, StreamObserver<BrabocoinProtos.HandshakeResponse> responseObserver) {
            HandshakeResponse response = new HandshakeResponse(environment.getPeers().stream().map(Peer::toString).collect(Collectors.toList()));
            responseObserver.onNext(Converter.create().toProtobuf(BrabocoinProtos.HandshakeResponse.class, response));
            responseObserver.onCompleted();
            // TODO: logging
        }

        @Override
        public void sendBlock(BrabocoinProtos.Hash request, StreamObserver<Empty> responseObserver) {
            Hash hash = Converter.create().toDomain(Hash.Builder.class, request).build();
            environment.onReceiveBlockHash(hash);
            responseObserver.onCompleted();
            // TODO: logging
        }

        @Override
        public void sendTransaction(BrabocoinProtos.Hash request, StreamObserver<Empty> responseObserver) {
            Hash hash = Converter.create().toDomain(Hash.Builder.class, request).build();
            environment.onReceiveTransaction(hash);
            responseObserver.onCompleted();
            // TODO: logging
        }

        @Override
        public StreamObserver<BrabocoinProtos.Hash> getBlocks(StreamObserver<BrabocoinProtos.Block> responseObserver) {
            return new StreamObserver<BrabocoinProtos.Hash>() {
                @Override
                public void onNext(BrabocoinProtos.Hash value) {
                    Hash hash = Converter.create().toDomain(Hash.Builder.class, value).build();
                    Block block = environment.getBlock(hash);
                    if (block == null) {
                        return;
                    }

                    // TODO: logging

                    responseObserver.onNext(Converter.create().toProtobuf(BrabocoinProtos.Block.class, block));
                }

                @Override
                public void onError(Throwable t) {
                    // TODO: logging
                }

                @Override
                public void onCompleted() {
                    responseObserver.onCompleted();
                    // TODO: logging
                }
            };
        }

        @Override
        public StreamObserver<BrabocoinProtos.Hash> getTransactions(StreamObserver<BrabocoinProtos.Transaction> responseObserver) {
            return new StreamObserver<BrabocoinProtos.Hash>() {
                @Override
                public void onNext(BrabocoinProtos.Hash value) {
                    Hash hash = Converter.create().toDomain(Hash.Builder.class, value).build();
                    Transaction transaction = environment.getTransaction(hash);
                    if (transaction == null) {
                        return;
                    }

                    // TODO: logging

                    responseObserver.onNext(Converter.create().toProtobuf(BrabocoinProtos.Transaction.class, transaction));
                }

                @Override
                public void onError(Throwable t) {
                    // TODO: logging
                }

                @Override
                public void onCompleted() {
                    responseObserver.onCompleted();
                    // TODO: logging
                }
            };
        }

        @Override
        public void seekTransactionPool(Empty request, StreamObserver<BrabocoinProtos.Hash> responseObserver) {
            Iterator<Hash> transactionIterator = environment.getTransactionIterator();
            for (Iterator<Hash> it = transactionIterator; it.hasNext(); ) {
                Hash h = it.next();

                responseObserver.onNext(Converter.create().toProtobuf(BrabocoinProtos.Hash.class, h));
            }

            responseObserver.onCompleted();
        }

        @Override
        public void discoverTopBlockHeight(Empty request, StreamObserver<BrabocoinProtos.BlockHeight> responseObserver) {
            BlockHeight blockHeight = new BlockHeight(environment.getTopBlockHeight());
            responseObserver.onNext(Converter.create().toProtobuf(BrabocoinProtos.BlockHeight.class, blockHeight));
            responseObserver.onCompleted();
        }

        @Override
        public void checkChainCompatible(BrabocoinProtos.Hash request, StreamObserver<BrabocoinProtos.ChainCompatibility> responseObserver) {
            Hash hash = Converter.create().toDomain(Hash.Builder.class, request).build();
            ChainCompatibility compatibility = new ChainCompatibility(environment.isChainCompatible(hash));
            responseObserver.onNext(Converter.create().toProtobuf(BrabocoinProtos.ChainCompatibility.class, compatibility));
            responseObserver.onCompleted();
        }

        @Override
        public void seekBlockchain(BrabocoinProtos.Hash request, StreamObserver<BrabocoinProtos.Hash> responseObserver) {
            Hash hash = Converter.create().toDomain(Hash.Builder.class, request).build();
            Iterator<Hash> blockIterator = environment.getBlocksAbove(hash);
            for (Iterator<Hash> it = blockIterator; it.hasNext(); ) {
                Hash h = it.next();

                responseObserver.onNext(Converter.create().toProtobuf(BrabocoinProtos.Hash.class, h));
            }

            responseObserver.onCompleted();
        }
    }
}
