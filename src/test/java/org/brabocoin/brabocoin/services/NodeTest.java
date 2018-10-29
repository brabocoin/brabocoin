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
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

class NodeTest {
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

        Logger log = Logger.getLogger("org.brabocoin");
        log.setLevel(Level.ALL);
        Handler handler = new ConsoleHandler();
        handler.setLevel(Level.ALL);

        log.addHandler(handler);
    }

    private Node generateNode(int port, BraboConfig config) throws DatabaseException {
        return generateNode(port, config, new BlockDatabase(new HashMapDB(), config));
    }

    private Node generateNode(int port, BraboConfig config, BlockDatabase blockDatabase) throws DatabaseException {
        Consensus nodeAconsensus = new Consensus();
        Blockchain nodeAblockchain = new Blockchain(blockDatabase, nodeAconsensus);
        ChainUTXODatabase nodeAutxoDatabase = new ChainUTXODatabase(new HashMapDB(), nodeAconsensus);
        UTXOProcessor nodeAutxoProcessor = new UTXOProcessor(nodeAutxoDatabase);
        BlockValidator nodeAblockValidator = new BlockValidator();
        PeerProcessor nodeApeerProcessor = new PeerProcessor(new HashSet<>(), config);
        TransactionProcessor nodeAtransactionProcessor = new TransactionProcessor(new TransactionValidator(),
                new TransactionPool(config, new Random()), nodeAutxoDatabase, new UTXODatabase(new HashMapDB()));
        BlockProcessor nodeAblockProcessor = new BlockProcessor(
                nodeAblockchain,
                nodeAutxoProcessor,
                nodeAtransactionProcessor,
                nodeAconsensus,
                nodeAblockValidator);

        return new Node(port, new NodeEnvironment(port,
                nodeAblockchain,
                nodeAutxoDatabase,
                nodeAblockProcessor,
                nodeAutxoProcessor,
                nodeApeerProcessor,
                config));
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

        nodeA.stop();
        nodeA.blockUntilShutdown();
        nodeB.stop();
        nodeB.blockUntilShutdown();

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
            database.storeBlock(b, true);
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

        nodeA.stop();
        nodeA.blockUntilShutdown();
        nodeB.stop();
        nodeB.blockUntilShutdown();
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
            database.storeBlock(b, true);
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

        nodeA.stop();
        nodeA.blockUntilShutdown();
        nodeB.stop();
        nodeB.blockUntilShutdown();
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
            database.storeBlock(b, true);
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

        nodeA.stop();
        nodeA.blockUntilShutdown();
        nodeB.stop();
        nodeB.blockUntilShutdown();
    }

    //
//    @Test
//    void getTransactionsTest() throws IOException, InterruptedException, DatabaseException {
//        BraboConfig config = new MockBraboConfig(defaultConfig) {
//            @Override
//            public List<String> bootstrapPeers() {
//                return new ArrayList<String>() {{
//                    add("localhost:8092");
//                }};
//            }
//        };
//
//        Map<Hash, Transaction> transactionPool = new HashMap<>();
//        List<Transaction> transactions = Simulation.repeatedBuilder(() -> Simulation.randomTransaction(5, 5), 2);
//        for (Transaction t : transactions) {
//            transactionPool.put(t.computeHash(), t);
//        }
//
//        Node nodeA = new Node(8091, new NodeEnvironment(new BlockDatabase(new HashMapDB(), config), transactionPool, config));
//
//        Node nodeB = new Node(8092, new NodeEnvironment(new BlockDatabase(new HashMapDB(), config), new HashMap<>(), new MockBraboConfig(defaultConfig) {
//            @Override
//            public List<String> bootstrapPeers() {
//                return new ArrayList<String>() {{
//                    add("localhost:8091");
//                }};
//            }
//        }));
//
//        nodeA.start();
//        nodeB.start();
//
//        Peer nodeBpeer = nodeB.environment.getPeers().iterator().next();
//        List<Transaction> receivedTransactions = new ArrayList<>();
//        final CountDownLatch finishLatch = new CountDownLatch(1);
//        StreamObserver<BrabocoinProtos.Hash> requestObserver = nodeBpeer.getAsyncStub().getTransactions(new StreamObserver<BrabocoinProtos.Transaction>() {
//            @Override
//            public void onNext(BrabocoinProtos.Transaction value) {
//                receivedTransactions.add(ProtoConverter.toDomain(value, Transaction.Builder.class));
//            }
//
//            @Override
//            public void onError(Throwable t) {
//                finishLatch.countDown();
//            }
//
//            @Override
//            public void onCompleted() {
//                finishLatch.countDown();
//            }
//        });
//
//        for (Transaction transaction : transactions) {
//            requestObserver.onNext(ProtoConverter.toProto(transaction.computeHash(), BrabocoinProtos.Hash.class));
//        }
//        requestObserver.onCompleted();
//
//        finishLatch.await(1, TimeUnit.MINUTES);
//
//        List<ByteString> receivedTransactionHashes = receivedTransactions.stream()
//                .map(Transaction::computeHash)
//                .map(Hash::getValue)
//                .collect(Collectors.toList());
//
//        assertEquals(transactions.size(), receivedTransactions.size());
//        for (Transaction transaction : transactions) {
//            assertTrue(receivedTransactionHashes.contains(transaction.computeHash().getValue()));
//        }
//
//        nodeA.stop();
//        nodeA.blockUntilShutdown();
//        nodeB.stop();
//        nodeB.blockUntilShutdown();
//    }
//
//    @Test
//    void getTransactionsNotFoundTest() throws IOException, InterruptedException, DatabaseException {
//        BraboConfig config = new MockBraboConfig(defaultConfig) {
//            @Override
//            public List<String> bootstrapPeers() {
//                return new ArrayList<String>() {{
//                    add("localhost:8092");
//                }};
//            }
//        };
//
//        Map<Hash, Transaction> transactionPool = new HashMap<>();
//        List<Transaction> transactions = Simulation.repeatedBuilder(() -> Simulation.randomTransaction(5, 5), 2);
//        for (Transaction t : transactions) {
//            transactionPool.put(t.computeHash(), t);
//        }
//
//        Node nodeA = new Node(8091, new NodeEnvironment(new BlockDatabase(new HashMapDB(), config), transactionPool, config));
//
//        Node nodeB = new Node(8092, new NodeEnvironment(new BlockDatabase(new HashMapDB(), config), new HashMap<>(), new MockBraboConfig(defaultConfig) {
//            @Override
//            public List<String> bootstrapPeers() {
//                return new ArrayList<String>() {{
//                    add("localhost:8091");
//                }};
//            }
//        }));
//
//        nodeA.start();
//        nodeB.start();
//
//        Peer nodeBpeer = nodeB.environment.getPeers().iterator().next();
//        List<Transaction> receivedTransactions = new ArrayList<>();
//        final CountDownLatch finishLatch = new CountDownLatch(1);
//        StreamObserver<BrabocoinProtos.Hash> requestObserver = nodeBpeer.getAsyncStub().getTransactions(new StreamObserver<BrabocoinProtos.Transaction>() {
//            @Override
//            public void onNext(BrabocoinProtos.Transaction value) {
//                receivedTransactions.add(ProtoConverter.toDomain(value, Transaction.Builder.class));
//            }
//
//            @Override
//            public void onError(Throwable t) {
//                finishLatch.countDown();
//            }
//
//            @Override
//            public void onCompleted() {
//                finishLatch.countDown();
//            }
//        });
//
//        for (int i = 0; i < random.nextInt(50); i++) {
//            // send random hash
//            requestObserver.onNext(ProtoConverter.toProto(Simulation.randomHash(), BrabocoinProtos.Hash.class));
//        }
//        requestObserver.onCompleted();
//
//        finishLatch.await(1, TimeUnit.MINUTES);
//
//        List<ByteString> receivedTransactionHashes = receivedTransactions.stream()
//                .map(Transaction::computeHash)
//                .map(Hash::getValue)
//                .collect(Collectors.toList());
//
//        assertEquals(0, receivedTransactions.size());
//        for (Transaction transaction : transactions) {
//            assertFalse(receivedTransactionHashes.contains(transaction.computeHash().getValue()));
//        }
//
//        nodeA.stop();
//        nodeA.blockUntilShutdown();
//        nodeB.stop();
//        nodeB.blockUntilShutdown();
//    }
//
//    @Test
//    void getTransactionsIntermediateInvalidTest() throws IOException, InterruptedException, DatabaseException {
//        BraboConfig config = new MockBraboConfig(defaultConfig) {
//            @Override
//            public List<String> bootstrapPeers() {
//                return new ArrayList<String>() {{
//                    add("localhost:8092");
//                }};
//            }
//        };
//
//        Map<Hash, Transaction> transactionPool = new HashMap<>();
//        List<Transaction> transactions = Simulation.repeatedBuilder(() -> Simulation.randomTransaction(5, 5), 2);
//        for (Transaction t : transactions) {
//            transactionPool.put(t.computeHash(), t);
//        }
//
//        Node nodeA = new Node(8091, new NodeEnvironment(new BlockDatabase(new HashMapDB(), config), transactionPool, config));
//
//        Node nodeB = new Node(8092, new NodeEnvironment(new BlockDatabase(new HashMapDB(), config), new HashMap<>(), new MockBraboConfig(defaultConfig) {
//            @Override
//            public List<String> bootstrapPeers() {
//                return new ArrayList<String>() {{
//                    add("localhost:8091");
//                }};
//            }
//        }));
//
//        nodeA.start();
//        nodeB.start();
//
//        Peer nodeBpeer = nodeB.environment.getPeers().iterator().next();
//        List<Transaction> receivedTransactions = new ArrayList<>();
//        final CountDownLatch finishLatch = new CountDownLatch(1);
//        StreamObserver<BrabocoinProtos.Hash> requestObserver = nodeBpeer.getAsyncStub().getTransactions(new StreamObserver<BrabocoinProtos.Transaction>() {
//            @Override
//            public void onNext(BrabocoinProtos.Transaction value) {
//                receivedTransactions.add(ProtoConverter.toDomain(value, Transaction.Builder.class));
//            }
//
//            @Override
//            public void onError(Throwable t) {
//                finishLatch.countDown();
//            }
//
//            @Override
//            public void onCompleted() {
//                finishLatch.countDown();
//            }
//        });
//
//        for (Transaction transaction : transactions) {
//            for (int i = 0; i < random.nextInt(10); i++) {
//                // send random hash
//                requestObserver.onNext(ProtoConverter.toProto(Simulation.randomHash(), BrabocoinProtos.Hash.class));
//            }
//
//            requestObserver.onNext(ProtoConverter.toProto(transaction.computeHash(), BrabocoinProtos.Hash.class));
//
//            for (int i = 0; i < random.nextInt(10); i++) {
//                // send random hash
//                requestObserver.onNext(ProtoConverter.toProto(Simulation.randomHash(), BrabocoinProtos.Hash.class));
//            }
//        }
//        requestObserver.onCompleted();
//
//        finishLatch.await(1, TimeUnit.MINUTES);
//
//        List<ByteString> receivedTransactionHashes = receivedTransactions.stream()
//                .map(Transaction::computeHash)
//                .map(Hash::getValue)
//                .collect(Collectors.toList());
//
//        assertEquals(transactions.size(), receivedTransactions.size());
//        for (Transaction transaction : transactions) {
//            assertTrue(receivedTransactionHashes.contains(transaction.computeHash().getValue()));
//        }
//
//        nodeA.stop();
//        nodeA.blockUntilShutdown();
//        nodeB.stop();
//        nodeB.blockUntilShutdown();
//    }
//
//    @Test
//    void seekTransactionPoolTest() throws DatabaseException, IOException, InterruptedException {
//        BraboConfig config = new MockBraboConfig(defaultConfig) {
//            @Override
//            public List<String> bootstrapPeers() {
//                return new ArrayList<String>() {{
//                    add("localhost:8092");
//                }};
//            }
//        };
//        List<Transaction> transactions = Simulation.repeatedBuilder(() -> Simulation.randomTransaction(20, 20), 100);
//        Map<Hash, Transaction> transactionPool = new HashMap<>();
//        transactions.stream().forEach(t -> transactionPool.put(t.computeHash(), t));
//
//
//        Node nodeA = new Node(8091, new NodeEnvironment(new BlockDatabase(new HashMapDB(), config), transactionPool, config));
//
//        Node nodeB = new Node(8092, new NodeEnvironment(new BlockDatabase(new HashMapDB(), config), new HashMap<>(), new MockBraboConfig(defaultConfig) {
//            @Override
//            public List<String> bootstrapPeers() {
//                return new ArrayList<String>() {{
//                    add("localhost:8091");
//                }};
//            }
//        }));
//
//        nodeA.start();
//        nodeB.start();
//
//        Peer nodeBpeer = nodeB.environment.getPeers().iterator().next();
//        Iterator<BrabocoinProtos.Hash> receivedHashStream = nodeBpeer.getBlockingStub().seekTransactionPool(
//                Empty.newBuilder().build()
//        );
//
//        List<Hash> receivedHashes = new ArrayList<>();
//        receivedHashStream.forEachRemaining(h -> receivedHashes.add(ProtoConverter.toDomain(h, Hash.Builder.class)));
//
//        assertEquals(transactions.size(), receivedHashes.size());
//        for (Transaction transaction : transactions) {
//            assertTrue(receivedHashes.contains(transaction.computeHash()));
//        }
//
//        nodeA.stop();
//        nodeA.blockUntilShutdown();
//        nodeB.stop();
//        nodeB.blockUntilShutdown();
//    }
//
    @Test
    void announceBlockTest() throws DatabaseException, IOException, InterruptedException {
        final CountDownLatch finishLatch = new CountDownLatch(1);
        BraboConfig config = new MockBraboConfig(defaultConfig) {
            @Override
            public List<String> bootstrapPeers() {
                return new ArrayList<>();
            }
        };
        Consensus nodeAconsensus = new Consensus();
        Blockchain nodeAblockchain = new Blockchain(new BlockDatabase(new HashMapDB(), config), nodeAconsensus);
        ChainUTXODatabase nodeAutxoDatabase = new ChainUTXODatabase(new HashMapDB(), nodeAconsensus);
        UTXOProcessor nodeAutxoProcessor = new UTXOProcessor(nodeAutxoDatabase);
        BlockValidator nodeAblockValidator = new BlockValidator();
        PeerProcessor nodeApeerProcessor = new PeerProcessor(new HashSet<>(), config);
        TransactionProcessor nodeAtransactionProcessor = new TransactionProcessor(new TransactionValidator(),
                new TransactionPool(config, new Random()), nodeAutxoDatabase, new UTXODatabase(new HashMapDB()));
        BlockProcessor nodeAblockProcessor = new BlockProcessor(
                nodeAblockchain,
                nodeAutxoProcessor,
                nodeAtransactionProcessor,
                nodeAconsensus,
                nodeAblockValidator);

        Node nodeA = new Node(8090, new NodeEnvironment(8090,
                nodeAblockchain,
                nodeAutxoDatabase,
                nodeAblockProcessor,
                nodeAutxoProcessor,
                nodeApeerProcessor,
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

        nodeA.stop();
        nodeA.blockUntilShutdown();
        nodeB.stop();
        nodeB.blockUntilShutdown();
    }

//    @Test
//    void announceBlockPropagationTest() throws DatabaseException, IOException, InterruptedException {
//        final CountDownLatch finishLatch = new CountDownLatch(1);
//        Node nodeA = new Node(8090, new NodeEnvironment(new HashMapDB(), new HashMap<>(), new MockBraboConfig(defaultConfig) {
//            @Override
//            public List<String> bootstrapPeers() {
//                return new ArrayList<>();
//            }
//        }) {
//            @Override
//            public void onReceiveBlockHash(Hash blockHash) {
//                finishLatch.countDown();
//            }
//        });
//
//        Node nodeB = new Node(8091, new NodeEnvironment(new HashMapDB(), new HashMap<>(), new MockBraboConfig(defaultConfig) {
//            @Override
//            public List<String> bootstrapPeers() {
//                return new ArrayList<String>() {{
//                    add("localhost:8090");
//                }};
//            }
//        }));
//
//        Node nodeC = new Node(8092, new NodeEnvironment(new HashMapDB(), new HashMap<>(), new MockBraboConfig(defaultConfig) {
//            @Override
//            public List<String> bootstrapPeers() {
//                return new ArrayList<String>() {{
//                    add("localhost:8091");
//                }};
//            }
//        }));
//
//        nodeA.start();
//        nodeB.start();
//        nodeC.start();
//
//        Hash announceHash = Simulation.randomHash();
//
//        Peer nodeCpeer = nodeC.environment.getPeers().iterator().next();
//        nodeCpeer.getAsyncStub().announceBlock(ProtoConverter.toProto(announceHash, BrabocoinProtos.Hash.class), new StreamObserver<Empty>() {
//            @Override
//            public void onNext(Empty value) {
//
//            }
//
//            @Override
//            public void onError(Throwable t) {
//
//            }
//
//            @Override
//            public void onCompleted() {
//
//            }
//        });
//
//        assertTrue(finishLatch.await(1, TimeUnit.MINUTES));
//
//        nodeA.stop();
//        nodeA.blockUntilShutdown();
//        nodeB.stop();
//        nodeB.blockUntilShutdown();
//        nodeC.stop();
//        nodeC.blockUntilShutdown();
//    }
//

    @Test
    void announceTransactionTest() throws DatabaseException, IOException, InterruptedException {
        final CountDownLatch finishLatch = new CountDownLatch(1);
        BraboConfig config = new MockBraboConfig(defaultConfig) {
            @Override
            public List<String> bootstrapPeers() {
                return new ArrayList<>();
            }
        };
        Consensus nodeAconsensus = new Consensus();
        Blockchain nodeAblockchain = new Blockchain(new BlockDatabase(new HashMapDB(), config), nodeAconsensus);
        ChainUTXODatabase nodeAutxoDatabase = new ChainUTXODatabase(new HashMapDB(), nodeAconsensus);
        UTXOProcessor nodeAutxoProcessor = new UTXOProcessor(nodeAutxoDatabase);
        BlockValidator nodeAblockValidator = new BlockValidator();
        PeerProcessor nodeApeerProcessor = new PeerProcessor(new HashSet<>(), config);
        TransactionProcessor nodeAtransactionProcessor = new TransactionProcessor(new TransactionValidator(),
                new TransactionPool(config, new Random()), nodeAutxoDatabase, new UTXODatabase(new HashMapDB()));
        BlockProcessor nodeAblockProcessor = new BlockProcessor(
                nodeAblockchain,
                nodeAutxoProcessor,
                nodeAtransactionProcessor,
                nodeAconsensus,
                nodeAblockValidator);

        Node nodeA = new Node(8090, new NodeEnvironment(8090,
                nodeAblockchain,
                nodeAutxoDatabase,
                nodeAblockProcessor,
                nodeAutxoProcessor,
                nodeApeerProcessor,
                config) {
            @Override
            public void onReceiveTransaction(@NotNull Hash transactionHash, List<Peer> peers) {
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

        nodeA.stop();
        nodeA.blockUntilShutdown();
        nodeB.stop();
        nodeB.blockUntilShutdown();
    }

    @Test
    void discoverTopBlockHeightTest() throws DatabaseException, IOException, InterruptedException {
        BraboConfig config = new MockBraboConfig(defaultConfig) {
            @Override
            public List<String> bootstrapPeers() {
                return new ArrayList<>();
            }
        };
        Consensus nodeAconsensus = new Consensus();
        Blockchain nodeAblockchain = new Blockchain(new BlockDatabase(new HashMapDB(), config), nodeAconsensus);
        ChainUTXODatabase nodeAutxoDatabase = new ChainUTXODatabase(new HashMapDB(), nodeAconsensus);
        UTXOProcessor nodeAutxoProcessor = new UTXOProcessor(nodeAutxoDatabase);
        BlockValidator nodeAblockValidator = new BlockValidator();
        PeerProcessor nodeApeerProcessor = new PeerProcessor(new HashSet<>(), config);
        TransactionProcessor nodeAtransactionProcessor = new TransactionProcessor(new TransactionValidator(),
                new TransactionPool(config, new Random()), nodeAutxoDatabase, new UTXODatabase(new HashMapDB()));
        BlockProcessor nodeAblockProcessor = new BlockProcessor(
                nodeAblockchain,
                nodeAutxoProcessor,
                nodeAtransactionProcessor,
                nodeAconsensus,
                nodeAblockValidator);

        Node nodeA = new Node(8090, new NodeEnvironment(8090,
                nodeAblockchain,
                nodeAutxoDatabase,
                nodeAblockProcessor,
                nodeAutxoProcessor,
                nodeApeerProcessor,
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

        nodeA.stop();
        nodeA.blockUntilShutdown();
        nodeB.stop();
        nodeB.blockUntilShutdown();
    }

    @Test
    void checkChainCompatibleTest() throws DatabaseException, IOException, InterruptedException {
        BraboConfig config = new MockBraboConfig(defaultConfig) {
            @Override
            public List<String> bootstrapPeers() {
                return new ArrayList<>();
            }
        };
        Consensus nodeAconsensus = new Consensus();
        Blockchain nodeAblockchain = new Blockchain(new BlockDatabase(new HashMapDB(), config), nodeAconsensus);
        ChainUTXODatabase nodeAutxoDatabase = new ChainUTXODatabase(new HashMapDB(), nodeAconsensus);
        UTXOProcessor nodeAutxoProcessor = new UTXOProcessor(nodeAutxoDatabase);
        BlockValidator nodeAblockValidator = new BlockValidator();
        PeerProcessor nodeApeerProcessor = new PeerProcessor(new HashSet<>(), config);
        TransactionProcessor nodeAtransactionProcessor = new TransactionProcessor(new TransactionValidator(),
                new TransactionPool(config, new Random()), nodeAutxoDatabase, new UTXODatabase(new HashMapDB()));
        BlockProcessor nodeAblockProcessor = new BlockProcessor(
                nodeAblockchain,
                nodeAutxoProcessor,
                nodeAtransactionProcessor,
                nodeAconsensus,
                nodeAblockValidator);

        Node nodeA = new Node(8090, new NodeEnvironment(8090,
                nodeAblockchain,
                nodeAutxoDatabase,
                nodeAblockProcessor,
                nodeAutxoProcessor,
                nodeApeerProcessor,
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

        nodeA.stop();
        nodeA.blockUntilShutdown();
        nodeB.stop();
        nodeB.blockUntilShutdown();
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
        Consensus nodeAconsensus = new Consensus();
        Blockchain nodeAblockchain = new Blockchain(new BlockDatabase(new HashMapDB(), config), nodeAconsensus);
        ChainUTXODatabase nodeAutxoDatabase = new ChainUTXODatabase(new HashMapDB(), nodeAconsensus);
        UTXOProcessor nodeAutxoProcessor = new UTXOProcessor(nodeAutxoDatabase);
        BlockValidator nodeAblockValidator = new BlockValidator();
        PeerProcessor nodeApeerProcessor = new PeerProcessor(new HashSet<>(), config);
        TransactionProcessor nodeAtransactionProcessor = new TransactionProcessor(new TransactionValidator(),
                new TransactionPool(config, new Random()), nodeAutxoDatabase, new UTXODatabase(new HashMapDB()));
        BlockProcessor nodeAblockProcessor = new BlockProcessor(
                nodeAblockchain,
                nodeAutxoProcessor,
                nodeAtransactionProcessor,
                nodeAconsensus,
                nodeAblockValidator);

        Node nodeA = new Node(8090, new NodeEnvironment(8090,
                nodeAblockchain,
                nodeAutxoDatabase,
                nodeAblockProcessor,
                nodeAutxoProcessor,
                nodeApeerProcessor,
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

        nodeA.stop();
        nodeA.blockUntilShutdown();
        nodeB.stop();
        nodeB.blockUntilShutdown();
    }

//    @Test
//    void announceTransactionPropagationTest() throws DatabaseException, IOException, InterruptedException {
//        final CountDownLatch finishLatch = new CountDownLatch(1);
//        Node nodeA = new Node(8090, new NodeEnvironment(new HashMapDB(), new HashMap<>(), new MockBraboConfig(defaultConfig) {
//            @Override
//            public List<String> bootstrapPeers() {
//                return new ArrayList<>();
//            }
//        }) {
//
//            @Override
//            public void onReceiveTransaction(@NotNull Hash transactionHash) {
//                finishLatch.countDown();
//            }
//        });
//
//        Node nodeB = new Node(8091, new NodeEnvironment(new HashMapDB(), new HashMap<>(), new MockBraboConfig(defaultConfig) {
//            @Override
//            public List<String> bootstrapPeers() {
//                return new ArrayList<String>() {{
//                    add("localhost:8090");
//                }};
//            }
//        }));
//
//        Node nodeC = new Node(8092, new NodeEnvironment(new HashMapDB(), new HashMap<>(), new MockBraboConfig(defaultConfig) {
//            @Override
//            public List<String> bootstrapPeers() {
//                return new ArrayList<String>() {{
//                    add("localhost:8091");
//                }};
//            }
//        }));
//
//        nodeA.start();
//        nodeB.start();
//        nodeC.start();
//
//        Hash announceHash = Simulation.randomHash();
//
//        Peer nodeCpeer = nodeC.environment.getPeers().iterator().next();
//        nodeCpeer.getAsyncStub().announceTransaction(ProtoConverter.toProto(announceHash, BrabocoinProtos.Hash.class), new StreamObserver<Empty>() {
//            @Override
//            public void onNext(Empty value) {
//
//            }
//
//            @Override
//            public void onError(Throwable t) {
//
//            }
//
//            @Override
//            public void onCompleted() {
//
//            }
//        });
//
//        assertTrue(finishLatch.await(1, TimeUnit.MINUTES));
//
//        nodeA.stop();
//        nodeA.blockUntilShutdown();
//        nodeB.stop();
//        nodeB.blockUntilShutdown();
//        nodeC.stop();
//        nodeC.blockUntilShutdown();
//    }
//
//    @Test
//    void announceBlockDuplicatePropagationTest() throws DatabaseException, IOException, InterruptedException {
//        final CountDownLatch finishLatch = new CountDownLatch(2);
//        Node nodeA = new Node(8090, new NodeEnvironment(new HashMapDB(), new HashMap<>(), new MockBraboConfig(defaultConfig) {
//            @Override
//            public List<String> bootstrapPeers() {
//                return new ArrayList<String>() {{
//                    add("localhost:8091");
//                }};
//            }
//        }) {
//
//            @Override
//            public void onReceiveBlockHash(@NotNull Hash blockHash) {
//                finishLatch.countDown();
//            }
//        });
//
//        Node nodeB = new Node(8091, new NodeEnvironment(new HashMapDB(), new HashMap<>(), new MockBraboConfig(defaultConfig) {
//            @Override
//            public List<String> bootstrapPeers() {
//                return new ArrayList<String>() {{
//                    add("localhost:8090");
//                }};
//            }
//        }));
//
//        Node nodeC = new Node(8092, new NodeEnvironment(new HashMapDB(), new HashMap<>(), new MockBraboConfig(defaultConfig) {
//            @Override
//            public List<String> bootstrapPeers() {
//                return new ArrayList<String>() {{
//                    add("localhost:8091");
//                }};
//            }
//        }));
//
//        nodeA.start();
//        nodeB.start();
//        nodeC.start();
//
//        Hash announceHash = Simulation.randomHash();
//
//        Optional<Peer> nodeCpeerO = nodeC.environment.getPeers()
//                .stream()
//                .filter(p -> p.getPort() == 8091)
//                .findFirst();
//
//        assertTrue(nodeCpeerO.isPresent());
//
//        Peer nodeCpeer = nodeCpeerO.get();
//
//        nodeCpeer.getAsyncStub().announceBlock(ProtoConverter.toProto(announceHash, BrabocoinProtos.Hash.class), new StreamObserver<Empty>() {
//            @Override
//            public void onNext(Empty value) {
//
//            }
//
//            @Override
//            public void onError(Throwable t) {
//
//            }
//
//            @Override
//            public void onCompleted() {
//
//            }
//        });
//
//        assertFalse(finishLatch.await(10, TimeUnit.SECONDS));
//
//        nodeA.stop();
//        nodeA.blockUntilShutdown();
//        nodeB.stop();
//        nodeB.blockUntilShutdown();
//        nodeC.stop();
//        nodeC.blockUntilShutdown();
//    }
//
//    @Test
//    void announceTransactionDuplicatePropagationTest() throws DatabaseException, IOException, InterruptedException {
//        final CountDownLatch finishLatch = new CountDownLatch(2);
//        Node nodeA = new Node(8090, new NodeEnvironment(new HashMapDB(), new HashMap<>(), new MockBraboConfig(defaultConfig) {
//            @Override
//            public List<String> bootstrapPeers() {
//                return new ArrayList<String>() {{
//                    add("localhost:8091");
//                }};
//            }
//        }) {
//
//            @Override
//            public void onReceiveTransaction(@NotNull Hash transactionHash) {
//                finishLatch.countDown();
//            }
//        });
//
//        Node nodeB = new Node(8091, new NodeEnvironment(new HashMapDB(), new HashMap<>(), new MockBraboConfig(defaultConfig) {
//            @Override
//            public List<String> bootstrapPeers() {
//                return new ArrayList<String>() {{
//                    add("localhost:8090");
//                }};
//            }
//        }));
//
//        Node nodeC = new Node(8092, new NodeEnvironment(new HashMapDB(), new HashMap<>(), new MockBraboConfig(defaultConfig) {
//            @Override
//            public List<String> bootstrapPeers() {
//                return new ArrayList<String>() {{
//                    add("localhost:8091");
//                }};
//            }
//        }));
//
//        nodeA.start();
//        nodeB.start();
//        nodeC.start();
//
//        Hash announceHash = Simulation.randomHash();
//
//        Optional<Peer> nodeCpeerO = nodeC.environment.getPeers()
//                .stream()
//                .filter(p -> p.getPort() == 8091)
//                .findFirst();
//
//        assertTrue(nodeCpeerO.isPresent());
//
//        Peer nodeCpeer = nodeCpeerO.get();
//
//        nodeCpeer.getAsyncStub().announceTransaction(ProtoConverter.toProto(announceHash, BrabocoinProtos.Hash.class), new StreamObserver<Empty>() {
//            @Override
//            public void onNext(Empty value) {
//
//            }
//
//            @Override
//            public void onError(Throwable t) {
//
//            }
//
//            @Override
//            public void onCompleted() {
//
//            }
//        });
//
//        assertFalse(finishLatch.await(10, TimeUnit.SECONDS));
//
//        nodeA.stop();
//        nodeA.blockUntilShutdown();
//        nodeB.stop();
//        nodeB.blockUntilShutdown();
//        nodeC.stop();
//        nodeC.blockUntilShutdown();
//    }
}
