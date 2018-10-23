package org.brabocoin.brabocoin.services;

import com.google.protobuf.Empty;
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
            responseObserver.onNext(ProtoConverter.toProto(response, BrabocoinProtos.HandshakeResponse.class));
            responseObserver.onCompleted();
            // TODO: logging
        }

        @Override
        public void announceBlock(BrabocoinProtos.Hash request, StreamObserver<Empty> responseObserver) {
            Hash hash = ProtoConverter.toDomain(request, Hash.Builder.class);
            environment.onReceiveBlockHash(hash);
            responseObserver.onCompleted();
            // TODO: logging
        }

        @Override
        public void announceTransaction(BrabocoinProtos.Hash request, StreamObserver<Empty> responseObserver) {
            Hash hash = ProtoConverter.toDomain(request, Hash.Builder.class);
            environment.onReceiveTransaction(hash);
            responseObserver.onCompleted();
            // TODO: logging
        }

        @Override
        public StreamObserver<BrabocoinProtos.Hash> getBlocks(StreamObserver<BrabocoinProtos.Block> responseObserver) {
            return new StreamObserver<BrabocoinProtos.Hash>() {
                @Override
                public void onNext(BrabocoinProtos.Hash value) {
                    Hash hash = ProtoConverter.toDomain(value, Hash.Builder.class);
                    Block block = environment.getBlock(hash);
                    if (block == null) {
                        return;
                    }

                    // TODO: logging

                    responseObserver.onNext(ProtoConverter.toProto(block, BrabocoinProtos.Block.class));
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
                    Hash hash = ProtoConverter.toDomain(value, Hash.Builder.class);
                    Transaction transaction = environment.getTransaction(hash);
                    if (transaction == null) {
                        return;
                    }

                    // TODO: logging

                    responseObserver.onNext(ProtoConverter.toProto(transaction, BrabocoinProtos.Transaction.class));
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

                responseObserver.onNext(ProtoConverter.toProto(h, BrabocoinProtos.Hash.class));
            }

            responseObserver.onCompleted();
        }

        @Override
        public void discoverTopBlockHeight(Empty request, StreamObserver<BrabocoinProtos.BlockHeight> responseObserver) {
            BlockHeight blockHeight = new BlockHeight(environment.getTopBlockHeight());
            responseObserver.onNext(ProtoConverter.toProto(blockHeight, BrabocoinProtos.BlockHeight.class));
            responseObserver.onCompleted();
        }

        @Override
        public void checkChainCompatible(BrabocoinProtos.Hash request, StreamObserver<BrabocoinProtos.ChainCompatibility> responseObserver) {
            Hash hash = ProtoConverter.toDomain(request, Hash.Builder.class);
            ChainCompatibility compatibility = new ChainCompatibility(environment.isChainCompatible(hash));
            responseObserver.onNext(ProtoConverter.toProto(compatibility, BrabocoinProtos.ChainCompatibility.class));
            responseObserver.onCompleted();
        }

        @Override
        public void seekBlockchain(BrabocoinProtos.Hash request, StreamObserver<BrabocoinProtos.Hash> responseObserver) {
            Hash hash = ProtoConverter.toDomain(request, Hash.Builder.class);
            Iterator<Hash> blockIterator = environment.getBlocksAbove(hash);
            for (Iterator<Hash> it = blockIterator; it.hasNext(); ) {
                Hash h = it.next();

                responseObserver.onNext(ProtoConverter.toProto(h, BrabocoinProtos.Hash.class));
            }

            responseObserver.onCompleted();
        }
    }
}
