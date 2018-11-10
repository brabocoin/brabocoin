package org.brabocoin.brabocoin.services;

import com.google.protobuf.ByteString;
import com.google.protobuf.Empty;
import io.grpc.stub.StreamObserver;
import org.brabocoin.brabocoin.chain.Blockchain;
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
import org.brabocoin.brabocoin.processor.BlockProcessor;
import org.brabocoin.brabocoin.processor.PeerProcessor;
import org.brabocoin.brabocoin.processor.TransactionProcessor;
import org.brabocoin.brabocoin.processor.UTXOProcessor;
import org.brabocoin.brabocoin.proto.model.BrabocoinProtos;
import org.brabocoin.brabocoin.testutil.MockBraboConfig;
import org.brabocoin.brabocoin.testutil.Simulation;
import org.brabocoin.brabocoin.util.ProtoConverter;
import org.brabocoin.brabocoin.validation.BlockValidator;
import org.brabocoin.brabocoin.validation.Consensus;
import org.brabocoin.brabocoin.validation.TransactionValidator;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

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
        defaultConfig = new MockBraboConfig(defaultConfig) {
            @Override
            public String blockStoreDirectory() {
                return "src/test/resources/" + super.blockStoreDirectory();
            }
        };
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

        assertEquals(3, greeter.environment.getPeers().size());

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

        BlockDatabase database = new BlockDatabase(new HashMapDB(), config);
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

        Peer nodeBpeer = nodeB.environment.getPeers().iterator().next();
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
            requestObserver.onNext(ProtoConverter.toProto(block.computeHash(), BrabocoinProtos.Hash.class));
        }
        requestObserver.onCompleted();

        finishLatch.await(1, TimeUnit.MINUTES);

        List<ByteString> receivedBlockHashes = receivedBlocks.stream()
                .map(Block::computeHash)
                .map(Hash::getValue)
                .collect(Collectors.toList());

        assertEquals(blocks.size(), receivedBlocks.size());
        for (Block block : blocks) {
            assertTrue(receivedBlockHashes.contains(block.computeHash().getValue()));
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

        BlockDatabase database = new BlockDatabase(new HashMapDB(), config);
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

        Peer nodeBpeer = nodeB.environment.getPeers().iterator().next();
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
                .map(Block::computeHash)
                .map(Hash::getValue)
                .collect(Collectors.toList());

        assertEquals(0, receivedBlocks.size());
        for (Block block : blocks) {
            assertFalse(receivedBlockHashes.contains(block.computeHash().getValue()));
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

        BlockDatabase database = new BlockDatabase(new HashMapDB(), config);
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

        Peer nodeBpeer = nodeB.environment.getPeers().iterator().next();
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

            requestObserver.onNext(ProtoConverter.toProto(block.computeHash(), BrabocoinProtos.Hash.class));

            for (int i = 0; i < random.nextInt(10); i++) {
                // send random hash
                requestObserver.onNext(ProtoConverter.toProto(Simulation.randomHash(), BrabocoinProtos.Hash.class));
            }
        }
        requestObserver.onCompleted();

        finishLatch.await(1, TimeUnit.MINUTES);

        List<ByteString> receivedBlockHashes = receivedBlocks.stream()
                .map(Block::computeHash)
                .map(Hash::getValue)
                .collect(Collectors.toList());

        assertEquals(blocks.size(), receivedBlocks.size());
        for (Block block : blocks) {
            assertTrue(receivedBlockHashes.contains(block.computeHash().getValue()));
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
        };
        Consensus consensus = new Consensus();
        Blockchain blockchain = new Blockchain(new BlockDatabase(new HashMapDB(), config), consensus);
        ChainUTXODatabase ChainUtxoDatabase = new ChainUTXODatabase(new HashMapDB(), consensus);
        UTXOProcessor utxoProcessor = new UTXOProcessor(ChainUtxoDatabase);
        BlockValidator blockValidator = new BlockValidator();
        PeerProcessor peerProcessor = new PeerProcessor(new HashSet<>(), config);
        TransactionPool transactionPool = new TransactionPool(config, new Random());
        TransactionProcessor transactionProcessor = new TransactionProcessor(new TransactionValidator(),
                transactionPool, ChainUtxoDatabase, new UTXODatabase(new HashMapDB()));
        BlockProcessor blockProcessor = new BlockProcessor(
                blockchain,
                utxoProcessor,
                transactionProcessor,
                consensus,
                blockValidator);

        Node nodeA = new Node(8090, new NodeEnvironment(8090,
                blockchain,
                blockProcessor,
                peerProcessor,
                transactionPool,
                transactionProcessor,
                config) {
            @Override
            public void onReceiveBlockHash(@NotNull Hash blockHash, List<Peer> peer) {
                finishLatch.countDown();
            }
        });

        Node nodeB = generateNode(8091, new MockBraboConfig(defaultConfig) {
            @Override
            public List<String> bootstrapPeers() {
                return new ArrayList<String>() {{
                    add("localhost:8090");
                }};
            }
        });

        nodeA.start();
        nodeB.start();

        Hash announceHash = Simulation.randomHash();

        Peer nodeBpeer = nodeB.environment.getPeers().iterator().next();
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

        nodeA.stopAndBlock();
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
        };
        Consensus consensus = new Consensus();
        Blockchain blockchain = new Blockchain(new BlockDatabase(new HashMapDB(), config), consensus);
        ChainUTXODatabase ChainUtxoDatabase = new ChainUTXODatabase(new HashMapDB(), consensus);
        UTXOProcessor utxoProcessor = new UTXOProcessor(ChainUtxoDatabase);
        BlockValidator blockValidator = new BlockValidator();
        PeerProcessor peerProcessor = new PeerProcessor(new HashSet<>(), config);
        TransactionPool transactionPool = new TransactionPool(config, new Random());
        TransactionProcessor transactionProcessor = new TransactionProcessor(new TransactionValidator(),
                transactionPool, ChainUtxoDatabase, new UTXODatabase(new HashMapDB()));
        BlockProcessor blockProcessor = new BlockProcessor(
                blockchain,
                utxoProcessor,
                transactionProcessor,
                consensus,
                blockValidator);

        Node nodeA = new Node(8090, new NodeEnvironment(8090,
                blockchain,
                blockProcessor,
                peerProcessor,
                transactionPool,
                transactionProcessor,
                config) {
            @Override
            public void onReceiveTransactionHash(@NotNull Hash transactionHash, List<Peer> peers) {
                finishLatch.countDown();
            }
        });

        Node nodeB = generateNode(8091, new MockBraboConfig(defaultConfig) {
            @Override
            public List<String> bootstrapPeers() {
                return new ArrayList<String>() {{
                    add("localhost:8090");
                }};
            }
        });

        nodeA.start();
        nodeB.start();

        Hash announceHash = Simulation.randomHash();

        Peer nodeBpeer = nodeB.environment.getPeers().iterator().next();
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

        nodeA.stopAndBlock();
        nodeB.stopAndBlock();
    }

    @Test
    void discoverTopBlockHeightTest() throws DatabaseException, IOException, InterruptedException {
        BraboConfig config = new MockBraboConfig(defaultConfig) {
            @Override
            public List<String> bootstrapPeers() {
                return new ArrayList<>();
            }
        };
        Consensus consensus = new Consensus();
        Blockchain blockchain = new Blockchain(new BlockDatabase(new HashMapDB(), config), consensus);
        ChainUTXODatabase ChainUtxoDatabase = new ChainUTXODatabase(new HashMapDB(), consensus);
        UTXOProcessor utxoProcessor = new UTXOProcessor(ChainUtxoDatabase);
        BlockValidator blockValidator = new BlockValidator();
        PeerProcessor peerProcessor = new PeerProcessor(new HashSet<>(), config);
        TransactionPool transactionPool = new TransactionPool(config, new Random());
        TransactionProcessor transactionProcessor = new TransactionProcessor(new TransactionValidator(),
                transactionPool, ChainUtxoDatabase, new UTXODatabase(new HashMapDB()));
        BlockProcessor blockProcessor = new BlockProcessor(
                blockchain,
                utxoProcessor,
                transactionProcessor,
                consensus,
                blockValidator);

        Node nodeA = new Node(8090, new NodeEnvironment(8090,
                blockchain,
                blockProcessor,
                peerProcessor,
                transactionPool,
                transactionProcessor,
                config) {
            @Override
            public int getTopBlockHeight() {
                return 1234;
            }
        });

        Node nodeB = generateNode(8091, new MockBraboConfig(defaultConfig) {
            @Override
            public List<String> bootstrapPeers() {
                return new ArrayList<String>() {{
                    add("localhost:8090");
                }};
            }
        });

        nodeA.start();
        nodeB.start();

        Peer nodeBpeer = nodeB.environment.getPeers().iterator().next();

        BlockHeight blockHeight = ProtoConverter.toDomain(nodeBpeer.getBlockingStub().discoverTopBlockHeight(Empty.newBuilder().build()), BlockHeight.Builder.class);

        assert blockHeight != null;

        assertEquals(1234, blockHeight.getHeight());

        nodeA.stopAndBlock();
        nodeB.stopAndBlock();
    }

    @Test
    void checkChainCompatibleTest() throws DatabaseException, IOException, InterruptedException {
        BraboConfig config = new MockBraboConfig(defaultConfig) {
            @Override
            public List<String> bootstrapPeers() {
                return new ArrayList<>();
            }
        };
        Consensus consensus = new Consensus();
        Blockchain blockchain = new Blockchain(new BlockDatabase(new HashMapDB(), config), consensus);
        ChainUTXODatabase ChainUtxoDatabase = new ChainUTXODatabase(new HashMapDB(), consensus);
        UTXOProcessor utxoProcessor = new UTXOProcessor(ChainUtxoDatabase);
        BlockValidator blockValidator = new BlockValidator();
        PeerProcessor peerProcessor = new PeerProcessor(new HashSet<>(), config);
        TransactionPool transactionPool = new TransactionPool(config, new Random());
        TransactionProcessor transactionProcessor = new TransactionProcessor(new TransactionValidator(),
                transactionPool, ChainUtxoDatabase, new UTXODatabase(new HashMapDB()));
        BlockProcessor blockProcessor = new BlockProcessor(
                blockchain,
                utxoProcessor,
                transactionProcessor,
                consensus,
                blockValidator);

        Node nodeA = new Node(8090, new NodeEnvironment(8090,
                blockchain,
                blockProcessor,
                peerProcessor,
                transactionPool,
                transactionProcessor,
                config) {
            @Override
            public boolean isChainCompatible(@NotNull Hash blockHash) {
                return false;
            }
        });

        Node nodeB = generateNode(8091, new MockBraboConfig(defaultConfig) {
            @Override
            public List<String> bootstrapPeers() {
                return new ArrayList<String>() {{
                    add("localhost:8090");
                }};
            }
        });

        nodeA.start();
        nodeB.start();

        Peer nodeBpeer = nodeB.environment.getPeers().iterator().next();

        ChainCompatibility chainCompatibility = ProtoConverter.toDomain(
                nodeBpeer.getBlockingStub().checkChainCompatible(
                        ProtoConverter.toProto(Simulation.randomHash(), BrabocoinProtos.Hash.class)
                ), ChainCompatibility.Builder.class);

        assert chainCompatibility != null;

        assertFalse(chainCompatibility.isCompatible());

        nodeA.stopAndBlock();
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
        };
        Consensus consensus = new Consensus();
        Blockchain blockchain = new Blockchain(new BlockDatabase(new HashMapDB(), config), consensus);
        ChainUTXODatabase ChainUtxoDatabase = new ChainUTXODatabase(new HashMapDB(), consensus);
        UTXOProcessor utxoProcessor = new UTXOProcessor(ChainUtxoDatabase);
        BlockValidator blockValidator = new BlockValidator();
        PeerProcessor peerProcessor = new PeerProcessor(new HashSet<>(), config);
        TransactionPool transactionPool = new TransactionPool(config, new Random());
        TransactionProcessor transactionProcessor = new TransactionProcessor(new TransactionValidator(),
                transactionPool, ChainUtxoDatabase, new UTXODatabase(new HashMapDB()));
        BlockProcessor blockProcessor = new BlockProcessor(
                blockchain,
                utxoProcessor,
                transactionProcessor,
                consensus,
                blockValidator);

        Node nodeA = new Node(8090, new NodeEnvironment(8090,
                blockchain,
                blockProcessor,
                peerProcessor,
                transactionPool,
                transactionProcessor,
                config) {
            @Override
            public List<Hash> getBlocksAbove(Hash blockHash) {
                return hashes;
            }
        });

        Node nodeB = generateNode(8091, new MockBraboConfig(defaultConfig) {
            @Override
            public List<String> bootstrapPeers() {
                return new ArrayList<String>() {{
                    add("localhost:8090");
                }};
            }
        });

        nodeA.start();
        nodeB.start();

        Peer nodeBpeer = nodeB.environment.getPeers().iterator().next();

        Iterator<BrabocoinProtos.Hash> hashIterator = nodeBpeer.getBlockingStub().seekBlockchain(ProtoConverter.toProto(Simulation.randomHash(), BrabocoinProtos.Hash.class));

        List<BrabocoinProtos.Hash> receivedProtoHashes = new ArrayList<>();
        for (Iterator<BrabocoinProtos.Hash> it = hashIterator; it.hasNext(); ) {
            BrabocoinProtos.Hash h = it.next();

            receivedProtoHashes.add(h);
        }

        List<Hash> receivedHashes = receivedProtoHashes.stream().map(h -> ProtoConverter.toDomain(h, Hash.Builder.class)).collect(Collectors.toList());

        for (Hash h : hashes) {
            assertTrue(receivedHashes.contains(h));
        }

        for (Hash h : receivedHashes) {
            assertTrue(hashes.contains(h));
        }

        nodeA.stopAndBlock();
        nodeB.stopAndBlock();
    }
}
