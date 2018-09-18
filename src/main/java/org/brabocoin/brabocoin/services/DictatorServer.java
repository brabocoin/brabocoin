package org.brabocoin.brabocoin.services;

import com.google.protobuf.ByteString;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.stub.StreamObserver;
import org.brabocoin.brabocoin.proto.model.BrabocoinProtos;
import org.brabocoin.brabocoin.proto.services.DictatorGrpc;

import java.io.IOException;

/**
 * @author Sten Wessel
 */
public class DictatorServer {

    private final int port;
    private final Server server;

    public DictatorServer(final int port) {
        this.port = port;
        this.server = ServerBuilder.forPort(port).addService(new DictatorService()).build();
    }

    public void start() throws IOException {
        server.start();

        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                DictatorServer.this.stop();
            }
        });
    }

    public void stop() {
        if (server != null) {
            server.shutdown();
        }
    }

    private void blockUntilShutdown() throws InterruptedException {
        if (server != null) {
            server.awaitTermination();
        }
    }

    public static void main(final String[] args) throws Exception {
        final DictatorServer server = new DictatorServer(8980);
        server.start();
        server.blockUntilShutdown();
    }

    private static class DictatorService extends DictatorGrpc.DictatorImplBase {

        @Override
        public void getBlock(final BrabocoinProtos.Hash request, final StreamObserver<BrabocoinProtos.Block> responseObserver) {
            final BrabocoinProtos.Hash prevBlock = BrabocoinProtos.Hash.newBuilder()
                    .setValue(ByteString.copyFromUtf8("Hallo"))
                    .build();

            final BrabocoinProtos.Hash merkleRoot = BrabocoinProtos.Hash.newBuilder()
                    .setValue(ByteString.copyFromUtf8("Merkle"))
                    .build();

            final BrabocoinProtos.Hash targetValue = BrabocoinProtos.Hash.newBuilder()
                    .setValue(ByteString.copyFromUtf8("moeilijk"))
                    .build();

            responseObserver.onNext(
                    BrabocoinProtos.Block.newBuilder()
                            .setPreviousBlockHash(prevBlock)
                            .setMerkleRoot(merkleRoot)
                            .setTargetValue(targetValue)
                            .setNonce(0L)
                            .setTimestamp(1L)
                    .build()
            );

            responseObserver.onCompleted();
        }
    }
}
