package org.brabocoin.brabocoin.services;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.stub.StreamObserver;
import org.brabocoin.brabocoin.node.Environment;
import org.brabocoin.brabocoin.proto.model.BrabocoinProtos;
import org.brabocoin.brabocoin.proto.services.HandshakeRequest;
import org.brabocoin.brabocoin.proto.services.HandshakeResponse;
import org.brabocoin.brabocoin.proto.services.NodeGrpc;

import java.io.IOException;

/**
 * @author Sten Wessel
 */
public class Node {
    /**
     * Service parameters
     */
    private final int listenPort;
    private final Server server;
    private Environment environment;

    /**
     * Client parameters
     */
    private final int connectPort;
    private final ManagedChannel channel;
    public final NodeGrpc.NodeBlockingStub blockingStub;
    public final NodeGrpc.NodeStub asyncStub;

    public Node(final int listenPort, final int connectPort) {
        this.listenPort = listenPort;
        this.connectPort = connectPort;

        this.server = ServerBuilder.forPort(listenPort)
                .addService(new NodeService()).build();

        this.channel = ManagedChannelBuilder.
                forAddress("localhost", connectPort)
                .usePlaintext()
                .build();
        this.blockingStub = NodeGrpc.newBlockingStub(channel);
        this.asyncStub = NodeGrpc.newStub(channel);
    }

    public void start() throws IOException {
        server.start();

        Runtime.getRuntime().addShutdownHook(new Thread(Node.this::stop));
    }

    public void stop() {
        if (server != null) {
            server.shutdown();
        }
    }

    public void blockUntilShutdown() throws InterruptedException {
        if (server != null) {
            server.awaitTermination();
        }
    }

    private class NodeService extends NodeGrpc.NodeImplBase {
        @Override
        public void handshake(HandshakeRequest request, StreamObserver<HandshakeResponse> responseObserver) {

        }

        @Override
        public void getBlock(final BrabocoinProtos.Hash request, final StreamObserver<BrabocoinProtos.Block> responseObserver) {
//            Block block = new Block(new Hash(request.getValue()),
//                    new Hash(ByteString.copyFromUtf8("hallooo")),
//                    new Hash(ByteString.copyFromUtf8("doei")),
//                    ByteString.copyFromUtf8("a"), 2L);
//
//            BrabocoinProtos.Block protoBlock = Converter.create().toProtobuf(BrabocoinProtos.Block.class, block);
//
//            responseObserver.onNext(protoBlock);
//
//            responseObserver.onCompleted();
        }
    }
}
