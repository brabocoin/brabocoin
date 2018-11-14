package org.brabocoin.brabocoin.services;

import org.brabocoin.brabocoin.exceptions.DatabaseException;
import org.brabocoin.brabocoin.model.Block;
import org.brabocoin.brabocoin.model.Hash;
import org.brabocoin.brabocoin.model.Transaction;
import org.brabocoin.brabocoin.node.config.BraboConfig;
import org.brabocoin.brabocoin.node.config.BraboConfigProvider;
import org.brabocoin.brabocoin.processor.BlockProcessor;
import org.brabocoin.brabocoin.testutil.MockBraboConfig;
import org.brabocoin.brabocoin.testutil.Simulation;
import org.brabocoin.brabocoin.validation.Consensus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.awaitility.Awaitility.await;
import static org.brabocoin.brabocoin.testutil.Simulation.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class NodeTest {
    static BraboConfig defaultConfig = BraboConfigProvider.getConfig().bind("brabo", BraboConfig.class);

    @BeforeAll
    static void setUp() {
        defaultConfig = new MockBraboConfig(defaultConfig) {
            @Override
            public String blockStoreDirectory() {
                return "src/test/resources/" + super.blockStoreDirectory();
            }

            @Override
            public String utxoStoreDirectory() {
                return "src/test/resources/" + super.utxoStoreDirectory();
            }

            @Override
            public List<String> bootstrapPeers() {
                return new ArrayList<>();
            }
        };
    }

    @AfterEach
    void afterEach() throws IOException {
        Files.walkFileTree(Paths.get("src/test/resources/data/"), new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                Files.delete(dir);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    /**
     * <em>Setup:</em>
     * Two nodes: A and B.
     * B has a block X unknown to A.
     * X is a block mined on top of the genesis block.
     * A's blockchain is empty and consists only of the genesis block.
     *
     * <em>Expected result:</em>
     * After announcing the block to A, A should eventually fetch the block from B and put it on the blockchain.
     */
    @Test
    void announceBlockToPeer() throws DatabaseException, IOException, InterruptedException {
        // Create default node A
        Node nodeA = generateNode(8090, new MockBraboConfig(defaultConfig) {
            @Override
            public String blockStoreDirectory() {
                return super.blockStoreDirectory() + "/nodeA";
            }
        });

        // Create a blockchain with a block mined on top of genesis block.
        Consensus consensus = new Consensus();

        Block newBlock = new Block(consensus.getGenesisBlock().getHash(),
                Simulation.randomHash(),
                Simulation.randomHash(),
                Simulation.randomBigInteger(),
                1,
                Simulation.repeatedBuilder(() -> Simulation.randomTransaction(0, 5), 20));
        Hash newBlockHash = newBlock.getHash();

        Node nodeB = generateNodeWithBlocks(8091, new MockBraboConfig(defaultConfig) {
            @Override
            public String blockStoreDirectory() {
                return super.blockStoreDirectory() + "/nodeB";
            }

            @Override
            public List<String> bootstrapPeers() {
                return Collections.singletonList("localhost:8090");
            }
        }, consensus, Collections.singletonList(newBlock));

        // Start nodes
        nodeA.start();
        nodeB.start();

        nodeB.environment.announceBlockRequest(nodeB.environment.getBlock(newBlockHash));

        await().atMost(20, TimeUnit.SECONDS)
                .until(() -> nodeA.environment.getTopBlockHeight() >= 1);

        assertNotNull(nodeA.environment.getBlock(newBlockHash));
        assertEquals(1, nodeA.environment.getTopBlockHeight());

        nodeA.stopAndBlock();
        nodeB.stopAndBlock();
    }

    /**
     * <em>Setup:</em>
     * Three nodes: A, B and C.
     * C has a block X unknown to A and B.
     * X is a block mined on top of the genesis block.
     * A and B's blockchains are empty and consist only of the genesis block.
     *
     * <em>Expected result:</em>
     * After announcing the block to B, B should eventually fetch the block from C and put it on the blockchain.
     * B should then announce the block to A, which should in turn fetch the block from B and add it to its local blockchain.
     */
    @Test
    void announceBlockPropagatedToPeer() throws DatabaseException, IOException, InterruptedException {
        // Create default node A
        Node nodeA = generateNode(8090, new MockBraboConfig(defaultConfig) {
            @Override
            public String blockStoreDirectory() {
                return super.blockStoreDirectory() + "/nodeA";
            }

            @Override
            public int targetPeerCount() {
                return 1;
            }
        });

        // Create a blockchain with a block mined on top of genesis block.
        Consensus consensus = new Consensus();

        Block newBlock = new Block(consensus.getGenesisBlock().getHash(),
                Simulation.randomHash(),
                Simulation.randomHash(),
                Simulation.randomBigInteger(),
                1,
                Simulation.repeatedBuilder(() -> Simulation.randomTransaction(0, 5), 20));
        Hash newBlockHash = newBlock.getHash();

        Node nodeB = generateNode(8091, new MockBraboConfig(defaultConfig) {
            @Override
            public String blockStoreDirectory() {
                return super.blockStoreDirectory() + "/nodeB";
            }

            @Override
            public List<String> bootstrapPeers() {
                return Collections.singletonList("localhost:8090");
            }
        });

        Node nodeC = generateNodeWithBlocks(8092, new MockBraboConfig(defaultConfig) {
            @Override
            public String blockStoreDirectory() {
                return super.blockStoreDirectory() + "/nodeC";
            }

            @Override
            public List<String> bootstrapPeers() {
                return Collections.singletonList("localhost:8091");
            }

            @Override
            public int targetPeerCount() {
                return 1;
            }
        }, consensus, Collections.singletonList(newBlock));

        // Start nodes
        nodeA.start();
        nodeB.start();
        nodeC.start();

        nodeC.environment.announceBlockRequest(nodeC.environment.getBlock(newBlockHash));

        await().atMost(20, TimeUnit.SECONDS)
                .until(() -> nodeA.environment.getTopBlockHeight() >= 1);

        assertNotNull(nodeA.environment.getBlock(newBlockHash));
        assertEquals(1, nodeA.environment.getTopBlockHeight());

        nodeA.stopAndBlock();
        nodeB.stopAndBlock();
        nodeC.stopAndBlock();
    }

    /**
     * <em>Setup:</em>
     * Two nodes: A and B.
     * B has transaction X unknown to A.
     * A's transaction pool is empty.
     *
     * <em>Expected result:</em>
     * After announcing the transaction to A, A should eventually fetch the transaction from B and put it in the transaction pool.
     */
    @Test
    void announceTransactionToPeer() throws DatabaseException, IOException, InterruptedException {
        // Create default node A
        Node nodeA = generateNode(8090, new MockBraboConfig(defaultConfig) {
            @Override
            public String blockStoreDirectory() {
                return super.blockStoreDirectory() + "/nodeA";
            }
        });

        // Create a blockchain with a block mined on top of genesis block.
        Consensus consensus = new Consensus();

        Transaction newTransaction = Simulation.randomTransaction(0, 5);
        Hash newTransactionHash = newTransaction.computeHash();

        Node nodeB = generateNodeWithTransactions(8091, new MockBraboConfig(defaultConfig) {
            @Override
            public String blockStoreDirectory() {
                return super.blockStoreDirectory() + "/nodeB";
            }

            @Override
            public List<String> bootstrapPeers() {
                return Collections.singletonList("localhost:8090");
            }
        }, consensus, Collections.singletonList(newTransaction));

        // Start nodes
        nodeA.start();
        nodeB.start();

        nodeB.environment.announceTransactionRequest(nodeB.environment.getTransaction(newTransactionHash));

        await().atMost(20, TimeUnit.SECONDS)
                .until(() -> nodeA.environment.getTransactionHashSet().contains(newTransactionHash));

        assertNotNull(nodeA.environment.getTransaction(newTransactionHash));
        assertEquals(1, nodeA.environment.getTransactionHashSet().size());

        nodeA.stopAndBlock();
        nodeB.stopAndBlock();
    }

    /**
     * <em>Setup:</em>
     * Three nodes: A, B and C.
     * C has a transaction X unknown to A and B.
     * A and B's transaction pools are empty.
     *
     * <em>Expected result:</em>
     * After announcing the transaction to B, B should eventually fetch the transaction from C and put it in in the transaction pool.
     * B should then announce the transaction to A, which should in turn fetch the transaction from B and add it to its transaction pool.
     */
    @Test
    void announceTransactionPropagatedToPeerLimitedPeers() throws DatabaseException, IOException, InterruptedException {
        // Create default node A
        Node nodeA = generateNode(8090, new MockBraboConfig(defaultConfig) {
            @Override
            public String blockStoreDirectory() {
                return super.blockStoreDirectory() + "/nodeA";
            }

            @Override
            public int targetPeerCount() {
                return 1;
            }
        });

        // Create a blockchain with a block mined on top of genesis block.
        Consensus consensus = new Consensus();

        Transaction newTransaction = Simulation.randomTransaction(0, 5);
        Hash newTransactionHash = newTransaction.computeHash();

        Node nodeB = generateNode(8091, new MockBraboConfig(defaultConfig) {
            @Override
            public String blockStoreDirectory() {
                return super.blockStoreDirectory() + "/nodeB";
            }

            @Override
            public List<String> bootstrapPeers() {
                return Collections.singletonList("localhost:8090");
            }
        });

        Node nodeC = generateNodeWithTransactions(8092, new MockBraboConfig(defaultConfig) {
            @Override
            public String blockStoreDirectory() {
                return super.blockStoreDirectory() + "/nodeC";
            }

            @Override
            public List<String> bootstrapPeers() {
                return Collections.singletonList("localhost:8091");
            }

            @Override
            public int targetPeerCount() {
                return 1;
            }
        }, consensus, Collections.singletonList(newTransaction));

        // Start nodes
        nodeA.start();
        nodeB.start();
        nodeC.start();

        nodeC.environment.announceTransactionRequest(nodeC.environment.getTransaction(newTransactionHash));

        await().atMost(30, TimeUnit.SECONDS)
                .until(() -> nodeA.environment.getTransactionHashSet().contains(newTransactionHash));

        assertNotNull(nodeA.environment.getTransaction(newTransactionHash));
        assertEquals(1, nodeA.environment.getTransactionHashSet().size());

        nodeA.stopAndBlock();
        nodeB.stopAndBlock();
        nodeC.stopAndBlock();
    }

    /**
     * <em>Setup:</em>
     * Three nodes: A, B and C.
     * C has a transaction X unknown to A and B.
     * A and B's transaction pools are empty.
     *
     * <em>Expected result:</em>
     * After announcing the transaction to B, B should eventually fetch the transaction from C and put it in in the transaction pool.
     * B should then announce the transaction to A, which should in turn fetch the transaction from B and add it to its transaction pool.
     */
    @Test
    void announceTransactionPropagatedToPeer() throws DatabaseException, IOException, InterruptedException {
        // Create default node A
        Node nodeA = generateNode(8090, new MockBraboConfig(defaultConfig) {
            @Override
            public String blockStoreDirectory() {
                return super.blockStoreDirectory() + "/nodeA";
            }
        });

        // Create a blockchain with a block mined on top of genesis block.
        Consensus consensus = new Consensus();

        Transaction newTransaction = Simulation.randomTransaction(0, 5);
        Hash newTransactionHash = newTransaction.computeHash();

        Node nodeB = generateNode(8091, new MockBraboConfig(defaultConfig) {
            @Override
            public String blockStoreDirectory() {
                return super.blockStoreDirectory() + "/nodeB";
            }

            @Override
            public List<String> bootstrapPeers() {
                return Collections.singletonList("localhost:8090");
            }
        });

        Node nodeC = generateNodeWithTransactions(8092, new MockBraboConfig(defaultConfig) {
            @Override
            public String blockStoreDirectory() {
                return super.blockStoreDirectory() + "/nodeC";
            }

            @Override
            public List<String> bootstrapPeers() {
                return Collections.singletonList("localhost:8091");
            }
        }, consensus, Collections.singletonList(newTransaction));

        // Start nodes
        nodeA.start();
        nodeB.start();
        nodeC.start();

        nodeC.environment.announceTransactionRequest(nodeC.environment.getTransaction(newTransactionHash));

        await().atMost(20, TimeUnit.SECONDS)
                .until(() -> nodeA.environment.getTransactionHashSet().contains(newTransactionHash));

        assertNotNull(nodeA.environment.getTransaction(newTransactionHash));
        assertEquals(1, nodeA.environment.getTransactionHashSet().size());

        nodeA.stopAndBlock();
        nodeB.stopAndBlock();
        nodeC.stopAndBlock();
    }

    /**
     * <em>Setup:</em>
     * Three nodes: A, B and C.
     * A has the longest blockchain.
     * B has the shortest blockchain.
     * C's blockchain length is between A's and B's blockchain length and has a fork with respect to A's blockchain.
     *
     * <em>Expected result:</em>
     * A, B and C's blockchains are identical and equal to A's blockchain.
     */
    @Test
    void updateBlockchain() throws DatabaseException, IOException, InterruptedException {
        // Create a blockchain with a block mined on top of genesis block.
        Consensus consensus = new Consensus();
        List<Block> chainA = Simulation.randomBlockChainGenerator(20, consensus.getGenesisBlock().getHash(), 1, 0, 5);

        Node nodeA = generateNodeWithBlocks(8090, new MockBraboConfig(defaultConfig) {
            @Override
            public String blockStoreDirectory() {
                return super.blockStoreDirectory() + "/nodeA";
            }
        }, new Consensus(), chainA);

        List<Block> chainB = IntStream.range(0, 8).mapToObj(chainA::get).collect(Collectors.toList());

        Node nodeB = generateNodeWithBlocks(8091, new MockBraboConfig(defaultConfig) {
            @Override
            public String blockStoreDirectory() {
                return super.blockStoreDirectory() + "/nodeB";
            }

            @Override
            public List<String> bootstrapPeers() {
                return Collections.singletonList("localhost:8090");
            }
        }, new Consensus(), chainB);

        List<Block> chainC = IntStream.range(0, 10).mapToObj(chainA::get).collect(Collectors.toList());
        List<Block> chainCfork = Simulation.randomBlockChainGenerator(8, chainC.get(chainC.size() - 1).getHash(), chainC.size(), 0, 5);

        chainC.addAll(chainCfork);

        Node nodeC = generateNodeWithBlocks(8092, new MockBraboConfig(defaultConfig) {
            @Override
            public String blockStoreDirectory() {
                return super.blockStoreDirectory() + "/nodeC";
            }

            @Override
            public List<String> bootstrapPeers() {
                return Collections.singletonList("localhost:8091");
            }
        }, new Consensus(), chainC);


        // Start nodes
        nodeA.start();
        nodeB.start();
        nodeC.start();

        await().atMost(30, TimeUnit.SECONDS)
                .until(() -> nodeA.environment.getTopBlockHeight() == nodeC.environment.getTopBlockHeight() &&
                        nodeA.environment.getTopBlockHeight() == nodeB.environment.getTopBlockHeight()
                );

        assertEquals(nodeA.environment.getTopBlockHeight(), chainA.size());
        assertEquals(nodeA.environment.getTopBlockHeight(), nodeB.environment.getTopBlockHeight());
        assertEquals(nodeA.environment.getTopBlockHeight(), nodeC.environment.getTopBlockHeight());

        assertEquals(chainA.get(chainA.size() - 1).getHash(),
                nodeC.environment.getBlocksAbove(chainA.get(chainA.size() - 2).getHash()).get(0));

        nodeA.stopAndBlock();
        nodeB.stopAndBlock();
        nodeC.stopAndBlock();
    }

    /**
     * <em>Setup:</em>
     * Two nodes A and B.
     * B's blockchain is in sync with A's blockchain.
     * A also has a fork that is one blockheight less than the main chain.
     *
     *
     * <em>Expected result:</em>
     * When we mine two blocks on top of the fork, only announcing the second block, B should sync with this fork, requesting both blocks.
     * Then, after announcing another two blocks on the other fork (what used to be the main chain), B should again switch the main chain.
     */
    @Test
    void forkSwitching() throws DatabaseException, IOException, InterruptedException {
        // Create a blockchain with a block mined on top of genesis block.
        Consensus consensus = new Consensus();
        List<Block> chainA = Simulation.randomBlockChainGenerator(20, consensus.getGenesisBlock().getHash(), 1, 0, 5);
        Block mainChainTopBlock = chainA.get(chainA.size() - 1);

        Node nodeB = generateNodeWithBlocks(8091, new MockBraboConfig(defaultConfig) {
            @Override
            public String blockStoreDirectory() {
                return super.blockStoreDirectory() + "/nodeB";
            }

            @Override
            public List<String> bootstrapPeers() {
                return Collections.singletonList("localhost:8090");
            }
        }, new Consensus(), chainA);


        Block forkMatchingBlock = chainA.get(10);
        List<Block> chainAfork = Simulation.randomBlockChainGenerator(8, forkMatchingBlock.getHash(), forkMatchingBlock.getBlockHeight() + 1, 0, 5);

        Block forkTopBlock = chainAfork.get(chainAfork.size() - 1);

        assert forkTopBlock.getBlockHeight() == mainChainTopBlock.getBlockHeight() - 1;

        chainA.addAll(chainAfork);
        Map.Entry<Node, BlockProcessor> fullNodeA = generateNodeAndProcessorWithBlocks(8090, new MockBraboConfig(defaultConfig) {
            @Override
            public String blockStoreDirectory() {
                return super.blockStoreDirectory() + "/nodeA";
            }
        }, new Consensus(), chainA);

        Node nodeA = fullNodeA.getKey();
        BlockProcessor blockProcessor = fullNodeA.getValue();

        nodeA.start();
        nodeB.start();

        assertEquals(nodeA.environment.getTopBlockHeight(), nodeB.environment.getTopBlockHeight());

        // Mine two block on top of fork of A
        Block forkBlock1 = Simulation.randomBlock(
                forkTopBlock.getHash(),
                forkTopBlock.getBlockHeight() + 1,
                0, 5, 20
        );
        Block forkBlock2 = Simulation.randomBlock(
                forkBlock1.getHash(),
                forkBlock1.getBlockHeight() + 1,
                0, 5, 20
        );

        blockProcessor.processNewBlock(forkBlock1);
        blockProcessor.processNewBlock(forkBlock2);

        assert forkBlock2.getBlockHeight() == mainChainTopBlock.getBlockHeight() + 1;

        nodeA.environment.announceBlockRequest(forkBlock2);

        await().atMost(30, TimeUnit.SECONDS)
                .until(() -> nodeA.environment.getTopBlockHeight() == nodeB.environment.getTopBlockHeight());

        assertEquals(
                nodeA.environment.getBlocksAbove(forkBlock1.getHash()).get(0),
                nodeB.environment.getBlocksAbove(forkBlock1.getHash()).get(0));

        // Mine two block on top of new fork of A, what used to be the main chain
        Block fork2Block1 = Simulation.randomBlock(
                mainChainTopBlock.getHash(),
                mainChainTopBlock.getBlockHeight() + 1,
                0, 5, 20
        );
        Block fork2Block2 = Simulation.randomBlock(
                fork2Block1.getHash(),
                fork2Block1.getBlockHeight() + 1,
                0, 5, 20
        );

        blockProcessor.processNewBlock(fork2Block1);
        blockProcessor.processNewBlock(fork2Block2);

        assert fork2Block2.getBlockHeight() > forkBlock2.getBlockHeight();

        nodeA.environment.announceBlockRequest(fork2Block2);

        await().atMost(30, TimeUnit.SECONDS)
                .until(() -> nodeA.environment.getTopBlockHeight() == nodeB.environment.getTopBlockHeight());

        assertEquals(
                nodeA.environment.getBlocksAbove(fork2Block1.getHash()).get(0),
                nodeB.environment.getBlocksAbove(fork2Block1.getHash()).get(0));


        nodeA.stopAndBlock();
        nodeB.stopAndBlock();
    }

    /**
     * <em>Setup:</em>
     * Three nodes A, B and C.
     * B's blockchain is in sync with A's blockchain.
     * C's blockchain is empty and only consists of the genesis block and only has peer B.
     * A also has a fork that is one blockheight less than the main chain.
     *
     *
     * <em>Expected result:</em>
     * When we mine two blocks on top of the fork, only announcing the second block, B should sync with this fork, requesting both blocks.
     * Then, after announcing another two blocks on the other fork (what used to be the main chain), B should again switch the main chain.
     */
    @Test
    void forkSwitchingPropagated() throws DatabaseException, IOException, InterruptedException {
        // Create a blockchain with a block mined on top of genesis block.
        Consensus consensus = new Consensus();
        List<Block> chainA = Simulation.randomBlockChainGenerator(20, consensus.getGenesisBlock().getHash(), 1, 0, 5);
        Block mainChainTopBlock = chainA.get(chainA.size() - 1);

        Node nodeB = generateNodeWithBlocks(8091, new MockBraboConfig(defaultConfig) {
            @Override
            public String blockStoreDirectory() {
                return super.blockStoreDirectory() + "/nodeB";
            }

            @Override
            public List<String> bootstrapPeers() {
                return Collections.singletonList("localhost:8090");
            }
        }, new Consensus(), chainA);


        Block forkMatchingBlock = chainA.get(10);
        List<Block> chainAfork = Simulation.randomBlockChainGenerator(8, forkMatchingBlock.getHash(), forkMatchingBlock.getBlockHeight() + 1, 0, 5);

        Block forkTopBlock = chainAfork.get(chainAfork.size() - 1);

        assert forkTopBlock.getBlockHeight() == mainChainTopBlock.getBlockHeight() - 1;

        chainA.addAll(chainAfork);
        Map.Entry<Node, BlockProcessor> fullNodeA = generateNodeAndProcessorWithBlocks(8090, new MockBraboConfig(defaultConfig) {
            @Override
            public String blockStoreDirectory() {
                return super.blockStoreDirectory() + "/nodeA";
            }

            @Override
            public int targetPeerCount() {
                return 1;
            }
        }, new Consensus(), chainA);

        Node nodeC = generateNode(8092, new MockBraboConfig(defaultConfig) {
            @Override
            public String blockStoreDirectory() {
                return super.blockStoreDirectory() + "/nodeC";
            }

            @Override
            public List<String> bootstrapPeers() {
                return Collections.singletonList("localhost:8091");
            }

            @Override
            public int targetPeerCount() {
                return 1;
            }
        });

        Node nodeA = fullNodeA.getKey();
        BlockProcessor blockProcessor = fullNodeA.getValue();

        nodeA.start();
        nodeB.start();
        nodeC.start();

        assertEquals(nodeA.environment.getTopBlockHeight(), nodeB.environment.getTopBlockHeight());

        // Mine two block on top of fork of A
        Block forkBlock1 = Simulation.randomBlock(
                forkTopBlock.getHash(),
                forkTopBlock.getBlockHeight() + 1,
                0, 5, 20
        );
        Block forkBlock2 = Simulation.randomBlock(
                forkBlock1.getHash(),
                forkBlock1.getBlockHeight() + 1,
                0, 5, 20
        );

        blockProcessor.processNewBlock(forkBlock1);
        blockProcessor.processNewBlock(forkBlock2);

        assert forkBlock2.getBlockHeight() == mainChainTopBlock.getBlockHeight() + 1;

        nodeA.environment.announceBlockRequest(forkBlock2);

        await().atMost(30, TimeUnit.SECONDS)
                .until(() -> nodeA.environment.getTopBlockHeight() == nodeC.environment.getTopBlockHeight());

        assertEquals(
                nodeA.environment.getBlocksAbove(forkBlock1.getHash()).get(0),
                nodeC.environment.getBlocksAbove(forkBlock1.getHash()).get(0));

        // Mine two block on top of new fork of A, what used to be the main chain
        Block fork2Block1 = Simulation.randomBlock(
                mainChainTopBlock.getHash(),
                mainChainTopBlock.getBlockHeight() + 1,
                0, 5, 20
        );
        Block fork2Block2 = Simulation.randomBlock(
                fork2Block1.getHash(),
                fork2Block1.getBlockHeight() + 1,
                0, 5, 20
        );

        blockProcessor.processNewBlock(fork2Block1);
        blockProcessor.processNewBlock(fork2Block2);

        assert fork2Block2.getBlockHeight() > forkBlock2.getBlockHeight();

        nodeA.environment.announceBlockRequest(fork2Block2);

        await().atMost(30, TimeUnit.SECONDS)
                .until(() -> nodeA.environment.getTopBlockHeight() == nodeC.environment.getTopBlockHeight());

        assertEquals(
                nodeA.environment.getBlocksAbove(fork2Block1.getHash()).get(0),
                nodeC.environment.getBlocksAbove(fork2Block1.getHash()).get(0));


        nodeA.stopAndBlock();
        nodeB.stopAndBlock();
        nodeC.stopAndBlock();
    }

    /**
     * node B tries to acquire the transaction pool from its peer A on startup.
     */
    @Test
    void seekTransctionPool() throws DatabaseException, IOException, InterruptedException {
        List<Transaction> transactions = Simulation.repeatedBuilder(() -> Simulation.randomTransaction(0, 5), 200);

        Node nodeA = generateNodeWithTransactions(8090, new MockBraboConfig(defaultConfig) {
            @Override
            public String blockStoreDirectory() {
                return super.blockStoreDirectory() + "/nodeA";
            }
        }, new Consensus(), transactions);

        Node nodeB = generateNode(8091, new MockBraboConfig(defaultConfig) {
            @Override
            public String blockStoreDirectory() {
                return super.blockStoreDirectory() + "/nodeB";
            }

            @Override
            public List<String> bootstrapPeers() {
                return Collections.singletonList("localhost:8090");
            }
        });

        // Start nodes
        nodeA.start();
        nodeB.start();

        await().atMost(30, TimeUnit.SECONDS)
                .until(() -> nodeB.environment.getTransactionHashSet().size() == transactions.size());

        nodeA.stopAndBlock();
        nodeB.stopAndBlock();
    }
}
