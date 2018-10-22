package org.brabocoin.brabocoin.services;

import com.google.protobuf.ByteString;
import com.google.protobuf.Empty;
import io.grpc.stub.StreamObserver;
import org.brabocoin.brabocoin.dal.BlockDatabase;
import org.brabocoin.brabocoin.dal.HashMapDB;
import org.brabocoin.brabocoin.exceptions.DatabaseException;
import org.brabocoin.brabocoin.model.Block;
import org.brabocoin.brabocoin.model.Hash;
import org.brabocoin.brabocoin.model.Transaction;
import org.brabocoin.brabocoin.node.NodeEnvironment;
import org.brabocoin.brabocoin.node.Peer;
import org.brabocoin.brabocoin.node.config.BraboConfig;
import org.brabocoin.brabocoin.node.config.BraboConfigProvider;
import org.brabocoin.brabocoin.proto.model.BrabocoinProtos;
import org.brabocoin.brabocoin.testutil.MockBraboConfig;
import org.brabocoin.brabocoin.testutil.Simulation;
import org.brabocoin.brabocoin.util.ProtoConverter;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class NodeTest {
    private Random random = new Random();
    static BraboConfig defaultConfig = new BraboConfigProvider().getConfig().bind("brabo", BraboConfig.class);

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
        Node nodeA = new Node(8091, new NodeEnvironment(new HashMapDB(), new HashMap<>(), new MockBraboConfig(defaultConfig) {
            @Override
            public List<String> bootstrapPeers() {
                return new ArrayList<>();
            }
        }));
        Node nodeB = new Node(8092, new NodeEnvironment(new HashMapDB(), new HashMap<>(), new MockBraboConfig(defaultConfig) {
            @Override
            public List<String> bootstrapPeers() {
                return new ArrayList<>();
            }
        }));

        Node responder = new Node(8090, new NodeEnvironment(new HashMapDB(), new HashMap<>(), new MockBraboConfig(defaultConfig) {
            @Override
            public List<String> bootstrapPeers() {
                return new ArrayList<String>() {{
                    add("localhost:8091");
                    add("localhost:8092");
                }};
            }
        }));
        Node greeter = new Node(8089, new NodeEnvironment(new HashMapDB(), new HashMap<>(), new MockBraboConfig(defaultConfig) {
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

        Node nodeA = new Node(8091, new NodeEnvironment(database, new HashMap<>(), config));

        Node nodeB = new Node(8092, new NodeEnvironment(new HashMapDB(), new HashMap<>(), new MockBraboConfig(defaultConfig) {
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

        Node nodeA = new Node(8091, new NodeEnvironment(database, new HashMap<>(), config));

        Node nodeB = new Node(8092, new NodeEnvironment(new HashMapDB(), new HashMap<>(), new MockBraboConfig(defaultConfig) {
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

        Node nodeA = new Node(8091, new NodeEnvironment(database, new HashMap<>(), config));

        Node nodeB = new Node(8092, new NodeEnvironment(new HashMapDB(), new HashMap<>(), new MockBraboConfig(defaultConfig) {
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

    @Test
    void getTransactionsTest() throws IOException, InterruptedException, DatabaseException {
        BraboConfig config = new MockBraboConfig(defaultConfig) {
            @Override
            public List<String> bootstrapPeers() {
                return new ArrayList<String>() {{
                    add("localhost:8092");
                }};
            }
        };

        Map<Hash, Transaction> transactionPool = new HashMap<>();
        List<Transaction> transactions = Simulation.repeatedBuilder(() -> Simulation.randomTransaction(5,5),2);
        for (Transaction t : transactions) {
            transactionPool.put(t.computeHash(),t);
        }

        Node nodeA = new Node(8091, new NodeEnvironment(new BlockDatabase(new HashMapDB(), config), transactionPool, config));

        Node nodeB = new Node(8092, new NodeEnvironment(new BlockDatabase(new HashMapDB(), config), new HashMap<>(), new MockBraboConfig(defaultConfig) {
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
        List<Transaction> receivedTransactions = new ArrayList<>();
        final CountDownLatch finishLatch = new CountDownLatch(1);
        StreamObserver<BrabocoinProtos.Hash> requestObserver = nodeBpeer.asyncStub.getTransactions(new StreamObserver<BrabocoinProtos.Transaction>() {
            @Override
            public void onNext(BrabocoinProtos.Transaction value) {
                receivedTransactions.add(ProtoConverter.toDomain(value, Transaction.Builder.class));
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

        for (Transaction transaction : transactions) {
            requestObserver.onNext(ProtoConverter.toProto(transaction.computeHash(), BrabocoinProtos.Hash.class));
        }
        requestObserver.onCompleted();

        finishLatch.await(1, TimeUnit.MINUTES);

        List<ByteString> receivedTransactionHashes = receivedTransactions.stream()
                .map(Transaction::computeHash)
                .map(Hash::getValue)
                .collect(Collectors.toList());

        assertEquals(transactions.size(), receivedTransactions.size());
        for (Transaction transaction : transactions) {
            assertTrue(receivedTransactionHashes.contains(transaction.computeHash().getValue()));
        }

        nodeA.stop();
        nodeA.blockUntilShutdown();
        nodeB.stop();
        nodeB.blockUntilShutdown();
    }

    @Test
    void getTransactionsNotFoundTest() throws IOException, InterruptedException, DatabaseException {
        BraboConfig config = new MockBraboConfig(defaultConfig) {
            @Override
            public List<String> bootstrapPeers() {
                return new ArrayList<String>() {{
                    add("localhost:8092");
                }};
            }
        };

        Map<Hash, Transaction> transactionPool = new HashMap<>();
        List<Transaction> transactions = Simulation.repeatedBuilder(() -> Simulation.randomTransaction(5,5),2);
        for (Transaction t : transactions) {
            transactionPool.put(t.computeHash(),t);
        }

        Node nodeA = new Node(8091, new NodeEnvironment(new BlockDatabase(new HashMapDB(), config), transactionPool, config));

        Node nodeB = new Node(8092, new NodeEnvironment(new BlockDatabase(new HashMapDB(), config), new HashMap<>(), new MockBraboConfig(defaultConfig) {
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
        List<Transaction> receivedTransactions = new ArrayList<>();
        final CountDownLatch finishLatch = new CountDownLatch(1);
        StreamObserver<BrabocoinProtos.Hash> requestObserver = nodeBpeer.asyncStub.getTransactions(new StreamObserver<BrabocoinProtos.Transaction>() {
            @Override
            public void onNext(BrabocoinProtos.Transaction value) {
                receivedTransactions.add(ProtoConverter.toDomain(value, Transaction.Builder.class));
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

        List<ByteString> receivedTransactionHashes = receivedTransactions.stream()
                .map(Transaction::computeHash)
                .map(Hash::getValue)
                .collect(Collectors.toList());

        assertEquals(0, receivedTransactions.size());
        for (Transaction transaction : transactions) {
            assertFalse(receivedTransactionHashes.contains(transaction.computeHash().getValue()));
        }

        nodeA.stop();
        nodeA.blockUntilShutdown();
        nodeB.stop();
        nodeB.blockUntilShutdown();
    }

    @Test
    void getTransactionsIntermediateInvalidTest() throws IOException, InterruptedException, DatabaseException {
        BraboConfig config = new MockBraboConfig(defaultConfig) {
            @Override
            public List<String> bootstrapPeers() {
                return new ArrayList<String>() {{
                    add("localhost:8092");
                }};
            }
        };

        Map<Hash, Transaction> transactionPool = new HashMap<>();
        List<Transaction> transactions = Simulation.repeatedBuilder(() -> Simulation.randomTransaction(5,5),2);
        for (Transaction t : transactions) {
            transactionPool.put(t.computeHash(),t);
        }

        Node nodeA = new Node(8091, new NodeEnvironment(new BlockDatabase(new HashMapDB(), config), transactionPool, config));

        Node nodeB = new Node(8092, new NodeEnvironment(new BlockDatabase(new HashMapDB(), config), new HashMap<>(), new MockBraboConfig(defaultConfig) {
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
        List<Transaction> receivedTransactions = new ArrayList<>();
        final CountDownLatch finishLatch = new CountDownLatch(1);
        StreamObserver<BrabocoinProtos.Hash> requestObserver = nodeBpeer.asyncStub.getTransactions(new StreamObserver<BrabocoinProtos.Transaction>() {
            @Override
            public void onNext(BrabocoinProtos.Transaction value) {
                receivedTransactions.add(ProtoConverter.toDomain(value, Transaction.Builder.class));
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

        for (Transaction transaction : transactions) {
            for (int i = 0; i < random.nextInt(10); i++) {
                // send random hash
                requestObserver.onNext(ProtoConverter.toProto(Simulation.randomHash(), BrabocoinProtos.Hash.class));
            }

            requestObserver.onNext(ProtoConverter.toProto(transaction.computeHash(), BrabocoinProtos.Hash.class));

            for (int i = 0; i < random.nextInt(10); i++) {
                // send random hash
                requestObserver.onNext(ProtoConverter.toProto(Simulation.randomHash(), BrabocoinProtos.Hash.class));
            }
        }
        requestObserver.onCompleted();

        finishLatch.await(1, TimeUnit.MINUTES);

        List<ByteString> receivedTransactionHashes = receivedTransactions.stream()
                .map(Transaction::computeHash)
                .map(Hash::getValue)
                .collect(Collectors.toList());

        assertEquals(transactions.size(), receivedTransactions.size());
        for (Transaction transaction : transactions) {
            assertTrue(receivedTransactionHashes.contains(transaction.computeHash().getValue()));
        }

        nodeA.stop();
        nodeA.blockUntilShutdown();
        nodeB.stop();
        nodeB.blockUntilShutdown();
    }

    @Test
    void seekTransactionPoolTest() throws DatabaseException, IOException, InterruptedException {
        BraboConfig config = new MockBraboConfig(defaultConfig) {
            @Override
            public List<String> bootstrapPeers() {
                return new ArrayList<String>() {{
                    add("localhost:8092");
                }};
            }
        };
        List<Transaction> transactions = Simulation.repeatedBuilder(() -> Simulation.randomTransaction(20, 20), 100);
        Map<Hash, Transaction> transactionPool = new HashMap<>();
        transactions.stream().forEach(t -> transactionPool.put(t.computeHash(), t));


        Node nodeA = new Node(8091, new NodeEnvironment(new BlockDatabase(new HashMapDB(), config), transactionPool, config));

        Node nodeB = new Node(8092, new NodeEnvironment(new BlockDatabase(new HashMapDB(), config), new HashMap<>(), new MockBraboConfig(defaultConfig) {
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
        Iterator<BrabocoinProtos.Hash> receivedHashStream = nodeBpeer.blockingStub.seekTransactionPool(
                Empty.newBuilder().build()
        );

        List<Hash> receivedHashes = new ArrayList<>();
        receivedHashStream.forEachRemaining(h -> receivedHashes.add(ProtoConverter.toDomain(h, Hash.Builder.class)));

        assertEquals(transactions.size(), receivedHashes.size());
        for (Transaction transaction : transactions) {
            assertTrue(receivedHashes.contains(transaction.computeHash()));
        }

        nodeA.stop();
        nodeA.blockUntilShutdown();
        nodeB.stop();
        nodeB.blockUntilShutdown();
    }
}
