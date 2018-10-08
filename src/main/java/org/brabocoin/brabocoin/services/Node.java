package org.brabocoin.brabocoin.services;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.stub.StreamObserver;
import net.badata.protobuf.converter.Converter;
import org.brabocoin.brabocoin.model.HandshakeResponse;
import org.brabocoin.brabocoin.node.NodeEnvironment;
import org.brabocoin.brabocoin.node.Peer;
import org.brabocoin.brabocoin.proto.model.BrabocoinProtos;
import org.brabocoin.brabocoin.proto.services.NodeGrpc;

import java.io.IOException;
import java.util.stream.Collectors;

/**
 * A full node on the Brabocoin network.
 */
public class Node {
    /**
     * Service parameters
     */
    private final int listenPort;
    private final Server server;
    NodeEnvironment environment;

    public Node(final int listenPort) {
        this.listenPort = listenPort;
        this.server = ServerBuilder.forPort(listenPort)
                .addService(new NodeService()).build();
    }

    NodeEnvironment createEnvironment() {
        return new NodeEnvironment();
    }

    public void start() throws IOException {
        this.environment = createEnvironment();
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
        public void handshake(BrabocoinProtos.HandshakeRequest request, StreamObserver<BrabocoinProtos.HandshakeResponse> responseObserver) {
            HandshakeResponse response = new HandshakeResponse(environment.getPeers().stream().map(Peer::toString).collect(Collectors.toList()));
            responseObserver.onNext(Converter.create().toProtobuf(BrabocoinProtos.HandshakeResponse.class, response));
            responseObserver.onCompleted();
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
