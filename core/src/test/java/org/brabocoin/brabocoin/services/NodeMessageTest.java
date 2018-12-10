package org.brabocoin.brabocoin.services;

import com.google.protobuf.ByteString;
import com.google.protobuf.Empty;
import io.grpc.stub.StreamObserver;
import org.brabocoin.brabocoin.dal.*;
import org.brabocoin.brabocoin.exceptions.DatabaseException;
import org.brabocoin.brabocoin.model.Block;
import org.brabocoin.brabocoin.model.Hash;
import org.brabocoin.brabocoin.model.messages.BlockHeight;
import org.brabocoin.brabocoin.model.messages.ChainCompatibility;
import org.brabocoin.brabocoin.node.NodeEnvironment;
import org.brabocoin.brabocoin.node.Peer;
import org.brabocoin.brabocoin.node.config.BraboConfig;
import org.brabocoin.brabocoin.node.config.BraboConfigProvider;
import org.brabocoin.brabocoin.node.state.State;
import org.brabocoin.brabocoin.proto.model.BrabocoinProtos;
import org.brabocoin.brabocoin.testutil.MockBraboConfig;
import org.brabocoin.brabocoin.testutil.Simulation;
import org.brabocoin.brabocoin.testutil.TestState;
import org.brabocoin.brabocoin.util.ProtoConverter;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.brabocoin.brabocoin.testutil.Simulation.generateNode;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

class NodeMessageTest {
    private Random random = new Random();
    static BraboConfig defaultConfig = BraboConfigProvider.getConfig().bind("brabo", BraboConfig.class);

    @BeforeAll
    static void setUp() {
        defaultConfig = new MockBraboConfig(defaultConfig);
    }

    @Test
    void handshakeTest() throws IOException, DatabaseException, InterruptedException {
        Node nodeA = generateNode(8091, new MockBraboConfig(defaultConfig) {
            @Override
            public List<String> bootstrapPeers() {
                return new ArrayList<>();
            }
        });


        Node nodeB = generateNode(8092, new MockBraboConfig(defaultConfig) {
            @Override
            public List<String> bootstrapPeers() {
                return new ArrayList<>();
            }
        });

        Node responder = generateNode(8090, new MockBraboConfig(defaultConfig) {
            @Override
            public List<String> bootstrapPeers() {
                return new ArrayList<String>() {{
                    add("localhost:8091");
                    add("localhost:8092");
                }};
            }
        });

        Node greeter = generateNode(8089, new MockBraboConfig(defaultConfig) {
            @Override
            public List<String> bootstrapPeers() {
                return new ArrayList<String>() {{
                    add("localhost:8090");
                }};
            }
        });

        nodeA.start();
        nodeB.start();

        responder.start();
        greeter.start();

        assertEquals(3, greeter.getEnvironment().getPeers().size());

        nodeA.stopAndBlock();
        nodeB.stopAndBlock();

        responder.stop();
        responder.blockUntilShutdown();
        greeter.stop();
        greeter.blockUntilShutdown();
    }


    @Test
    void getBlocksTest() throws DatabaseException, IOException, InterruptedException {
        BraboConfig config = new MockBraboConfig(defaultConfig) {
            @Override
            public List<String> bootstrapPeers() {
                return new ArrayList<String>() {{
                    add("localhost:8092");
                }};
            }
        };

        BlockDatabase database = new BlockDatabase(new HashMapDB(), new File(config.blockStoreDirectory()), config.maxBlockFileSize());
        List<Block> blocks = Simulation.randomBlockChainGenerator(2);
        for (Block b : blocks) {
            database.storeBlock(b);
        }

        Node nodeA = generateNode(8091, config, database);

        Node nodeB = generateNode(8092, new MockBraboConfig(defaultConfig) {
            @Override
            public List<String> bootstrapPeers() {
                return new ArrayList<String>() {{
                    add("localhost:8091");
                }};
            }
        });

        nodeA.start();
        nodeB.start();

        Peer nodeBpeer = nodeB.getEnvironment().getPeers().iterator().next();
        List<Block> receivedBlocks = new ArrayList<>();
        final CountDownLatch finishLatch = new CountDownLatch(1);
        StreamObserver<BrabocoinProtos.Hash> requestObserver = nodeBpeer.getAsyncStub().getBlocks(new StreamObserver<BrabocoinProtos.Block>() {
            @Override
            public void onNext(BrabocoinProtos.Block value) {
                receivedBlocks.add(ProtoConverter.toDomain(value, Block.Builder.class));
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
            requestObserver.onNext(ProtoConverter.toProto(block.getHash(), BrabocoinProtos.Hash.class));
        }
        requestObserver.onCompleted();

        finishLatch.await(1, TimeUnit.MINUTES);

        List<ByteString> receivedBlockHashes = receivedBlocks.stream()
                .map(Block::getHash)
                .map(Hash::getValue)
                .collect(Collectors.toList());

        assertEquals(blocks.size(), receivedBlocks.size());
        for (Block block : blocks) {
            assertTrue(receivedBlockHashes.contains(block.getHash().getValue()));
        }

        nodeA.stopAndBlock();
        nodeB.stopAndBlock();
    }

    @Test
    void getBlocksNotFoundTest() throws DatabaseException, IOException, InterruptedException {
        BraboConfig config = new MockBraboConfig(defaultConfig) {
            @Override
            public List<String> bootstrapPeers() {
                return new ArrayList<String>() {{
                    add("localhost:8092");
                }};
            }
        };

        BlockDatabase database = new BlockDatabase(new HashMapDB(), new File(config.blockStoreDirectory()), config.maxBlockFileSize());
        List<Block> blocks = Simulation.randomBlockChainGenerator(2);
        for (Block b : blocks) {
            database.storeBlock(b);
        }

        Node nodeA = generateNode(8091, config, database);

        Node nodeB = generateNode(8092, new MockBraboConfig(defaultConfig) {
            @Override
            public List<String> bootstrapPeers() {
                return new ArrayList<String>() {{
                    add("localhost:8091");
                }};
            }
        });

        nodeA.start();
        nodeB.start();

        Peer nodeBpeer = nodeB.getEnvironment().getPeers().iterator().next();
        List<Block> receivedBlocks = new ArrayList<>();
        final CountDownLatch finishLatch = new CountDownLatch(1);
        StreamObserver<BrabocoinProtos.Hash> requestObserver = nodeBpeer.getAsyncStub().getBlocks(new StreamObserver<BrabocoinProtos.Block>() {
            @Override
            public void onNext(BrabocoinProtos.Block value) {
                receivedBlocks.add(ProtoConverter.toDomain(value, Block.Builder.class));
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

        for (int i = 0; i < random.nextInt(50); i++) {
            // send random hash
            requestObserver.onNext(ProtoConverter.toProto(Simulation.randomHash(), BrabocoinProtos.Hash.class));
        }
        requestObserver.onCompleted();

        finishLatch.await(1, TimeUnit.MINUTES);

        List<ByteString> receivedBlockHashes = receivedBlocks.stream()
                .map(Block::getHash)
                .map(Hash::getValue)
                .collect(Collectors.toList());

        assertEquals(0, receivedBlocks.size());
        for (Block block : blocks) {
            assertFalse(receivedBlockHashes.contains(block.getHash().getValue()));
        }

        nodeA.stopAndBlock();
        nodeB.stopAndBlock();
    }

    @Test
    void getBlocksIntermediateInvalidTest() throws DatabaseException, IOException, InterruptedException {
        BraboConfig config = new MockBraboConfig(defaultConfig) {
            @Override
            public List<String> bootstrapPeers() {
                return new ArrayList<String>() {{
                    add("localhost:8092");
                }};
            }
        };

        BlockDatabase database = new BlockDatabase(new HashMapDB(), new File(config.blockStoreDirectory()), config.maxBlockFileSize());
        List<Block> blocks = Simulation.randomBlockChainGenerator(2);
        for (Block b : blocks) {
            database.storeBlock(b);
        }

        Node nodeA = generateNode(8091, config, database);

        Node nodeB = generateNode(8092, new MockBraboConfig(defaultConfig) {
            @Override
            public List<String> bootstrapPeers() {
                return new ArrayList<String>() {{
                    add("localhost:8091");
                }};
            }
        });

        nodeA.start();
        nodeB.start();

        Peer nodeBpeer = nodeB.getEnvironment().getPeers().iterator().next();
        List<Block> receivedBlocks = new ArrayList<>();
        final CountDownLatch finishLatch = new CountDownLatch(1);
        StreamObserver<BrabocoinProtos.Hash> requestObserver = nodeBpeer.getAsyncStub().getBlocks(new StreamObserver<BrabocoinProtos.Block>() {
            @Override
            public void onNext(BrabocoinProtos.Block value) {
                receivedBlocks.add(ProtoConverter.toDomain(value, Block.Builder.class));
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
            for (int i = 0; i < random.nextInt(10); i++) {
                // send random hash
                requestObserver.onNext(ProtoConverter.toProto(Simulation.randomHash(), BrabocoinProtos.Hash.class));
            }

            requestObserver.onNext(ProtoConverter.toProto(block.getHash(), BrabocoinProtos.Hash.class));

            for (int i = 0; i < random.nextInt(10); i++) {
                // send random hash
                requestObserver.onNext(ProtoConverter.toProto(Simulation.randomHash(), BrabocoinProtos.Hash.class));
            }
        }
        requestObserver.onCompleted();

        finishLatch.await(1, TimeUnit.MINUTES);

        List<ByteString> receivedBlockHashes = receivedBlocks.stream()
                .map(Block::getHash)
                .map(Hash::getValue)
                .collect(Collectors.toList());

        assertEquals(blocks.size(), receivedBlocks.size());
        for (Block block : blocks) {
            assertTrue(receivedBlockHashes.contains(block.getHash().getValue()));
        }

        nodeA.stopAndBlock();
        nodeB.stopAndBlock();
    }

    @Test
    void announceBlockTest() throws DatabaseException, IOException, InterruptedException {
        final CountDownLatch finishLatch = new CountDownLatch(1);
        BraboConfig config = new MockBraboConfig(defaultConfig) {
            @Override
            public List<String> bootstrapPeers() {
                return new ArrayList<>();
            }

            @Override
            public int servicePort() {
                return 8090;
            }
        };

        State stateA = new TestState(config) {
            @Override
            protected NodeEnvironment createEnvironment() {
                return new NodeEnvironment(this) {
                    @Override
                    public void onReceiveBlockHash(@NotNull Hash blockHash, List<Peer> peer) {
                        finishLatch.countDown();
                    }
                };
            }
        };

        Node nodeB = generateNode(8091, new MockBraboConfig(defaultConfig) {
            @Override
            public List<String> bootstrapPeers() {
                return new ArrayList<String>() {{
                    add("localhost:8090");
                }};
            }
        });

        stateA.getNode().start();
        nodeB.start();

        Hash announceHash = Simulation.randomHash();

        Peer nodeBpeer = nodeB.getEnvironment().getPeers().iterator().next();
        nodeBpeer.getAsyncStub().announceBlock(ProtoConverter.toProto(announceHash, BrabocoinProtos.Hash.class), new StreamObserver<Empty>() {
            @Override
            public void onNext(Empty value) {

            }

            @Override
            public void onError(Throwable t) {

            }

            @Override
            public void onCompleted() {

            }
        });

        assertTrue(finishLatch.await(1, TimeUnit.MINUTES));

        stateA.getNode().stopAndBlock();
        nodeB.stopAndBlock();
    }

    @Test
    void announceTransactionTest() throws DatabaseException, IOException, InterruptedException {
        final CountDownLatch finishLatch = new CountDownLatch(1);
        BraboConfig config = new MockBraboConfig(defaultConfig) {
            @Override
            public List<String> bootstrapPeers() {
                return new ArrayList<>();
            }

            @Override
            public int servicePort() {
                return 8090;
            }
        };

        State stateA = new TestState(config) {
            @Override
            protected NodeEnvironment createEnvironment() {
                return new NodeEnvironment(this) {
                    @Override
                    public void onReceiveTransactionHash(@NotNull Hash transactionHash, List<Peer> peers) {
                        finishLatch.countDown();
                    }
                };
            }
        };

        Node nodeB = generateNode(8091, new MockBraboConfig(defaultConfig) {
            @Override
            public List<String> bootstrapPeers() {
                return new ArrayList<String>() {{
                    add("localhost:8090");
                }};
            }
        });

        stateA.getNode().start();
        nodeB.start();

        Hash announceHash = Simulation.randomHash();

        Peer nodeBpeer = nodeB.getEnvironment().getPeers().iterator().next();
        nodeBpeer.getAsyncStub().announceTransaction(ProtoConverter.toProto(announceHash, BrabocoinProtos.Hash.class), new StreamObserver<Empty>() {
            @Override
            public void onNext(Empty value) {

            }

            @Override
            public void onError(Throwable t) {

            }

            @Override
            public void onCompleted() {

            }
        });

        assertTrue(finishLatch.await(1, TimeUnit.MINUTES));

        stateA.getNode().stopAndBlock();
        nodeB.stopAndBlock();
    }

    @Test
    void discoverTopBlockHeightTest() throws DatabaseException, IOException, InterruptedException {
        BraboConfig config = new MockBraboConfig(defaultConfig) {
            @Override
            public List<String> bootstrapPeers() {
                return new ArrayList<>();
            }

            @Override
            public int servicePort() {
                return 8090;
            }
        };

        State stateA = new TestState(config) {
            @Override
            protected NodeEnvironment createEnvironment() {
                return new NodeEnvironment(this) {
                    @Override
                    public int getTopBlockHeight() {
                        return 1234;
                    }
                };
            }
        };

        Node nodeB = generateNode(8091, new MockBraboConfig(defaultConfig) {
            @Override
            public List<String> bootstrapPeers() {
                return new ArrayList<String>() {{
                    add("localhost:8090");
                }};
            }
        });

        stateA.getNode().start();
        nodeB.start();

        Peer nodeBpeer = nodeB.getEnvironment().getPeers().iterator().next();

        BlockHeight blockHeight = ProtoConverter.toDomain(nodeBpeer.getBlockingStub().discoverTopBlockHeight(Empty.newBuilder().build()), BlockHeight.Builder.class);

        assert blockHeight != null;

        assertEquals(1234, blockHeight.getHeight());

        stateA.getNode().stopAndBlock();
        nodeB.stopAndBlock();
    }

    @Test
    void checkChainCompatibleTest() throws DatabaseException, IOException, InterruptedException {
        BraboConfig config = new MockBraboConfig(defaultConfig) {
            @Override
            public List<String> bootstrapPeers() {
                return new ArrayList<>();
            }

            @Override
            public int servicePort() {
                return 8090;
            }
        };

        State stateA = new TestState(config) {
            @Override
            protected NodeEnvironment createEnvironment() {
                return new NodeEnvironment(this) {
                    @Override
                    public boolean isChainCompatible(@NotNull Hash blockHash) {
                        return false;
                    }
                };
            }
        };

        Node nodeB = generateNode(8091, new MockBraboConfig(defaultConfig) {
            @Override
            public List<String> bootstrapPeers() {
                return new ArrayList<String>() {{
                    add("localhost:8090");
                }};
            }
        });

        stateA.getNode().start();
        nodeB.start();

        Peer nodeBpeer = nodeB.getEnvironment().getPeers().iterator().next();

        ChainCompatibility chainCompatibility = ProtoConverter.toDomain(
                nodeBpeer.getBlockingStub().checkChainCompatible(
                        ProtoConverter.toProto(Simulation.randomHash(), BrabocoinProtos.Hash.class)
                ), ChainCompatibility.Builder.class);

        assert chainCompatibility != null;

        assertFalse(chainCompatibility.isCompatible());

        stateA.getNode().stopAndBlock();
        nodeB.stopAndBlock();
    }

    @Test
    void seekBlockchainTest() throws DatabaseException, IOException, InterruptedException {
        List<Hash> hashes = Simulation.repeatedBuilder(Simulation::randomHash, 10);

        BraboConfig config = new MockBraboConfig(defaultConfig) {
            @Override
            public List<String> bootstrapPeers() {
                return new ArrayList<>();
            }

            @Override
            public int servicePort() {
                return 8090;
            }
        };

        State stateA = new TestState(config) {
            @Override
            protected NodeEnvironment createEnvironment() {
                return new NodeEnvironment(this) {
                    @Override
                    public List<Hash> getBlocksAbove(Hash blockHash) {
                        return hashes;
                    }
                };
            }
        };

        Node nodeB = generateNode(8091, new MockBraboConfig(defaultConfig) {
            @Override
            public List<String> bootstrapPeers() {
                return new ArrayList<String>() {{
                    add("localhost:8090");
                }};
            }
        });

        stateA.getNode().start();
        nodeB.start();

        Peer nodeBpeer = nodeB.getEnvironment().getPeers().iterator().next();

        Iterator<BrabocoinProtos.Hash> hashIterator = nodeBpeer.getBlockingStub().seekBlockchain(ProtoConverter.toProto(Simulation.randomHash(), BrabocoinProtos.Hash.class));

        List<BrabocoinProtos.Hash> receivedProtoHashes = new ArrayList<>();
        for (Iterator<BrabocoinProtos.Hash> it = hashIterator; it.hasNext(); ) {
            BrabocoinProtos.Hash h = it.next();

            receivedProtoHashes.add(h);
        }

        List<Hash> receivedHashes = receivedProtoHashes.stream().map(h -> (Hash)ProtoConverter.toDomain(h, Hash.Builder.class)).collect(Collectors.toList());

        for (Hash h : hashes) {
            assertTrue(receivedHashes.contains(h));
        }

        for (Hash h : receivedHashes) {
            assertTrue(hashes.contains(h));
        }

        stateA.getNode().stopAndBlock();
        nodeB.stopAndBlock();
    }
}
