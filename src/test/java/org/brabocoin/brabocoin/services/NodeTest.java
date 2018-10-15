package org.brabocoin.brabocoin.services;

import com.google.protobuf.ByteString;
import io.grpc.stub.StreamObserver;
import net.badata.protobuf.converter.Converter;
import org.brabocoin.brabocoin.exceptions.DatabaseException;
import org.brabocoin.brabocoin.model.Block;
import org.brabocoin.brabocoin.model.Hash;
import org.brabocoin.brabocoin.node.Peer;
import org.brabocoin.brabocoin.node.config.BraboConfig;
import org.brabocoin.brabocoin.node.config.BraboConfigProvider;
import org.brabocoin.brabocoin.proto.model.BrabocoinProtos;
import org.brabocoin.brabocoin.testutil.MockBraboConfig;
import org.brabocoin.brabocoin.testutil.MockEnvironment;
import org.brabocoin.brabocoin.testutil.MockNode;
import org.brabocoin.brabocoin.testutil.Simulation;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NodeTest {
    BraboConfig defaultConfig = new BraboConfigProvider().getConfig().bind("brabo", BraboConfig.class);

    @Test
    void handshakeTest() throws IOException, DatabaseException {
        Node nodeA = new MockNode(8091, new MockEnvironment(new MockBraboConfig(defaultConfig) {
            @Override
            public List<String> bootstrapPeers() {
                return new ArrayList<>();
            }
        }));
        Node nodeB = new MockNode(8092, new MockEnvironment(new MockBraboConfig(defaultConfig) {
            @Override
            public List<String> bootstrapPeers() {
                return new ArrayList<>();
            }
        }));

        Node responder = new MockNode(8090, new MockEnvironment(new MockBraboConfig(defaultConfig) {
            @Override
            public List<String> bootstrapPeers() {
                return new ArrayList<String>() {{
                    add("localhost:8091");
                    add("localhost:8092");
                }};
            }
        }));
        Node greeter = new MockNode(8089, new MockEnvironment(new MockBraboConfig(defaultConfig) {
            @Override
            public List<String> bootstrapPeers() {
                return new ArrayList<String>() {{
                    add("localhost:8090");
                }};
            }
        }));

        nodeA.start();
        nodeB.start();

        responder.start();
        greeter.start();

        assertEquals(3, greeter.environment.getPeers().size());

        nodeA.stop();
        nodeB.stop();

        responder.stop();
        greeter.stop();
    }

    @Test
    void getBlocksTest() throws DatabaseException, IOException, InterruptedException {
        List<Block> blocks = Simulation.randomBlockChainGenerator(2);
        Node nodeA = new MockNode(8091, new MockEnvironment(new MockBraboConfig(defaultConfig) {
            @Override
            public List<String> bootstrapPeers() {
                return new ArrayList<String>() {{
                    add("localhost:8092");
                }};
            }
        }, blocks));

        Node nodeB = new MockNode(8092, new MockEnvironment(new MockBraboConfig(defaultConfig) {
            @Override
            public List<String> bootstrapPeers() {
                return new ArrayList<String>() {{
                    add("localhost:8091");
                }};
            }
        }));

        nodeA.start();
        nodeB.start();

        Peer nodeBpeer = nodeB.environment.getPeers().get(0);
        List<Block> receivedBlocks = new ArrayList<>();
        final CountDownLatch finishLatch = new CountDownLatch(1);
        StreamObserver<BrabocoinProtos.Hash> requestObserver = nodeBpeer.asyncStub.getBlocks(new StreamObserver<BrabocoinProtos.Block>() {
            @Override
            public void onNext(BrabocoinProtos.Block value) {
                receivedBlocks.add(Converter.create().toDomain(Block.Builder.class, value).createBlock());
            }

            @Override
            public void onError(Throwable t) {
                finishLatch.countDown();
            }

            @Override
            public void onCompleted() {
                finishLatch.countDown();
            }
        });

        for (Block block : blocks) {
            requestObserver.onNext(Converter.create().toProtobuf(BrabocoinProtos.Hash.class, block.computeHash()));
        }
        requestObserver.onCompleted();

        finishLatch.await(1, TimeUnit.MINUTES);

        List<ByteString> receivedBlockHashes = receivedBlocks.stream()
                .map(Block::computeHash)
                .map(Hash::getValue)
                .collect(Collectors.toList());

        assertEquals(2, receivedBlocks.size());
        for (Block block : blocks) {
            assertTrue(receivedBlockHashes.contains(block.computeHash().getValue()));
        }
    }
}