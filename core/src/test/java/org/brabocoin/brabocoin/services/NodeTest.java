package org.brabocoin.brabocoin.services;

import com.google.protobuf.ByteString;
import org.brabocoin.brabocoin.chain.IndexedBlock;
import org.brabocoin.brabocoin.crypto.PublicKey;
import org.brabocoin.brabocoin.exceptions.DatabaseException;
import org.brabocoin.brabocoin.mining.Miner;
import org.brabocoin.brabocoin.model.Block;
import org.brabocoin.brabocoin.model.Hash;
import org.brabocoin.brabocoin.model.Input;
import org.brabocoin.brabocoin.model.Output;
import org.brabocoin.brabocoin.model.Transaction;
import org.brabocoin.brabocoin.model.UnsignedTransaction;
import org.brabocoin.brabocoin.node.config.BraboConfig;
import org.brabocoin.brabocoin.node.config.BraboConfigProvider;
import org.brabocoin.brabocoin.node.state.State;
import org.brabocoin.brabocoin.testutil.MockBraboConfig;
import org.brabocoin.brabocoin.testutil.Simulation;
import org.brabocoin.brabocoin.testutil.TestState;
import org.brabocoin.brabocoin.validation.Consensus;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class NodeTest {

    static BraboConfig defaultConfig = BraboConfigProvider.getConfig()
        .bind("brabo", BraboConfig.class);
    static Consensus mockConsensus;

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

        mockConsensus = new Consensus() {
            @Override
            public @NotNull Hash getTargetValue() {
                return new Hash(ByteString.copyFrom(
                    BigInteger.valueOf(2).pow(255).toByteArray()
                ));
            }
        };
    }

    @AfterEach
    void afterEach() throws IOException {
        Files.walkFileTree(Paths.get("src/test/resources/data/"), new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file,
                                             BasicFileAttributes attrs) throws IOException {
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir,
                                                      IOException exc) throws IOException {
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
     * After announcing the block to A, A should eventually fetch the block from B and put it on
     * the blockchain.
     */
    @Test
    void announceBlockToPeer() throws DatabaseException, IOException, InterruptedException {
        // Create default node A
        State stateA = new TestState(new MockBraboConfig(defaultConfig) {
            @Override
            public String blockStoreDirectory() {
                return super.blockStoreDirectory() + "/nodeA";
            }

            @Override
            public int servicePort() {
                return 8090;
            }
        }) {
            @Override
            protected Consensus createConsensus() {
                return mockConsensus;
            }
        };

        Miner minerA = stateA.getMiner();
        Block newBlock = minerA.mineNewBlock(stateA.getBlockchain()
            .getMainChain()
            .getGenesisBlock(), Simulation.randomHash());
        stateA.getBlockProcessor().processNewBlock(newBlock);

        Hash newBlockHash = newBlock.getHash();

        State stateB = new TestState(new MockBraboConfig(defaultConfig) {
            @Override
            public String blockStoreDirectory() {
                return super.blockStoreDirectory() + "/nodeB";
            }

            @Override
            public List<String> bootstrapPeers() {
                return Collections.singletonList("localhost:8090");
            }

            @Override
            public int servicePort() {
                return 8091;
            }
        }) {
            @Override
            protected Consensus createConsensus() {
                return mockConsensus;
            }
        };

        // Start nodes
        stateA.getNode().start();
        stateB.getNode().start();

        stateA.getEnvironment()
            .announceBlockRequest(stateA.getEnvironment().getBlock(newBlockHash));

        await().atMost(20, TimeUnit.SECONDS)
            .until(() -> stateB.getEnvironment().getTopBlockHeight() >= 1);

        assertNotNull(stateB.getEnvironment().getBlock(newBlockHash));
        assertEquals(1, stateB.getEnvironment().getTopBlockHeight());

        stateA.getNode().stopAndBlock();
        stateB.getNode().stopAndBlock();
    }

    /**
     * <em>Setup:</em>
     * Three nodes: A, B and C.
     * C has a block X unknown to A and B.
     * X is a block mined on top of the genesis block.
     * A and B's blockchains are empty and consist only of the genesis block.
     *
     * <em>Expected result:</em>
     * After announcing the block to B, B should eventually fetch the block from C and put it on
     * the blockchain.
     * B should then announce the block to A, which should in turn fetch the block from B and add
     * it to its local blockchain.
     */
    @Test
    void announceBlockPropagatedToPeer() throws DatabaseException, IOException,
                                                InterruptedException {
        // Create default node A
        State stateA = new TestState(new MockBraboConfig(defaultConfig) {
            @Override
            public String blockStoreDirectory() {
                return super.blockStoreDirectory() + "/nodeA";
            }

            @Override
            public int servicePort() {
                return 8090;
            }

            @Override
            public int targetPeerCount() {
                return 1;
            }
        }) {
            @Override
            protected Consensus createConsensus() {
                return mockConsensus;
            }
        };

        // Create a blockchain with a block mined on top of genesis block.
        Miner minerA = stateA.getMiner();
        Block newBlock = minerA.mineNewBlock(stateA.getBlockchain()
            .getMainChain()
            .getGenesisBlock(), Simulation.randomHash());
        Hash newBlockHash = newBlock.getHash();
        stateA.getBlockProcessor().processNewBlock(newBlock);

        State stateB = new TestState(new MockBraboConfig(defaultConfig) {
            @Override
            public String blockStoreDirectory() {
                return super.blockStoreDirectory() + "/nodeB";
            }

            @Override
            public int servicePort() {
                return 8091;
            }

            @Override
            public List<String> bootstrapPeers() {
                return Collections.singletonList("localhost:8090");
            }
        }) {
            @Override
            protected Consensus createConsensus() {
                return mockConsensus;
            }
        };

        State stateC = new TestState(new MockBraboConfig(defaultConfig) {
            @Override
            public String blockStoreDirectory() {
                return super.blockStoreDirectory() + "/nodeC";
            }

            @Override
            public int servicePort() {
                return 8092;
            }

            @Override
            public List<String> bootstrapPeers() {
                return Collections.singletonList("localhost:8091");
            }

            @Override
            public int targetPeerCount() {
                return 1;
            }
        }) {
            @Override
            protected Consensus createConsensus() {
                return mockConsensus;
            }
        };


        // Start nodes
        stateA.getNode().start();
        stateB.getNode().start();
        stateC.getNode().start();

        stateA.getEnvironment()
            .announceBlockRequest(stateA.getEnvironment().getBlock(newBlockHash));

        await().atMost(20, TimeUnit.SECONDS)
            .until(() -> stateC.getEnvironment().getTopBlockHeight() >= 1);

        assertNotNull(stateC.getEnvironment().getBlock(newBlockHash));
        assertEquals(1, stateC.getEnvironment().getTopBlockHeight());

        stateA.getNode().stopAndBlock();
        stateB.getNode().stopAndBlock();
        stateC.getNode().stopAndBlock();
    }

    /**
     * <em>Setup:</em>
     * Two nodes: A and B.
     * B has transaction X unknown to A.
     * A's transaction pool is empty.
     *
     * <em>Expected result:</em>
     * After announcing the transaction to A, A should eventually fetch the transaction
     * from B and put it in the transaction pool.
     */
    @Test
    void announceTransactionToPeer() throws DatabaseException, IOException,
                                            InterruptedException {
        // Create default node A
        State stateA = new TestState(new MockBraboConfig(defaultConfig) {
            @Override
            public String blockStoreDirectory() {
                return super.blockStoreDirectory() + "/nodeA";
            }

            @Override
            public int servicePort() {
                return 8090;
            }
        }) {
            @Override
            protected Consensus createConsensus() {
                return mockConsensus;
            }
        };


        // Mine 100 blocks to create valid output for new transaction
        BigInteger privateKey = Simulation.randomPrivateKey();
        PublicKey publicKey = stateA.getConsensus()
            .getCurve()
            .getPublicKeyFromPrivateKey(privateKey);

        Miner minerA = stateA.getMiner();

        IndexedBlock previousBlock = stateA.getBlockchain()
            .getMainChain()
            .getGenesisBlock();
        Hash coinbaseOutputAddress = publicKey.getHash();

        for (int i = 0; i < mockConsensus.getCoinbaseMaturityDepth() + 1; i++) {
            Block newBlock = minerA.mineNewBlock(previousBlock, coinbaseOutputAddress);
            stateA.getBlockProcessor().processNewBlock(newBlock);

            previousBlock = stateA.getBlockchain().getIndexedBlock(newBlock.getHash());
            coinbaseOutputAddress = Simulation.randomHash();
        }

        IndexedBlock afterGenesisIndexed = stateA.getBlockchain()
            .getMainChain()
            .getBlockAtHeight(1);
        assertNotNull(afterGenesisIndexed);
        Block afterGenesis = stateA.getBlockchain().getBlock(afterGenesisIndexed);

        UnsignedTransaction utx = new UnsignedTransaction(
            Collections.singletonList(new Input(
                afterGenesis.getCoinbaseTransaction().getHash(),
                0
            )),
            Collections.singletonList(
                new Output(Simulation.randomHash(), mockConsensus.getBlockReward() - 1)
            )
        );

        Transaction tx = utx.sign(
            Collections.singletonList(stateA.getSigner().signMessage(
                utx.getSignableTransactionData(), privateKey
            ))
        );

        State stateB = new TestState(new MockBraboConfig(defaultConfig) {
            @Override
            public String blockStoreDirectory() {
                return super.blockStoreDirectory() + "/nodeB";
            }

            @Override
            public List<String> bootstrapPeers() {
                return Collections.singletonList(
                    "localhost:8090"
                );
            }

            @Override
            public int servicePort() {
                return 8091;
            }
        }) {
            @Override
            protected Consensus createConsensus() {
                return mockConsensus;
            }
        };

        // Start nodes
        stateA.getNode().start();
        stateB.getNode().start();


        await().atMost(50, TimeUnit.SECONDS)
            .until(() -> stateB.getBlockchain().getMainChain().getHeight() == 101);

        stateA.getTransactionProcessor().processNewTransaction(tx);

        stateA.getEnvironment().announceTransactionRequest(stateA.getEnvironment()
            .getTransaction(tx.getHash()));

        await().atMost(20, TimeUnit.SECONDS)
            .until(() -> stateB.getEnvironment().getTransactionHashSet().contains(tx.getHash()));

        assertNotNull(stateB.getEnvironment().getTransaction(tx.getHash()));
        assertEquals(1, stateB.getEnvironment().getTransactionHashSet().size());

        stateA.getNode().stopAndBlock();
        stateB.getNode().stopAndBlock();
    }


    /**
     * <em>Setup:</em>
     * Three nodes: A, B and C.
     * C has a transaction X unknown to A and B.
     * A and B's transaction pools are empty.
     *
     * <em>Expected result:</em>
     * After announcing the transaction to B, B should eventually fetch the transaction
     * from C and put it in in the transaction pool.
     * B should then announce the transaction to A, which should in turn fetch the
     * transaction from B and add it to its transaction pool.
     */
    @Test
    void announceTransactionPropagatedToPeerLimitedPeers() throws DatabaseException,
                                                                  IOException,
                                                                  InterruptedException {
        // Create default node A
        State stateA = new TestState(new MockBraboConfig(defaultConfig) {
            @Override
            public String blockStoreDirectory() {
                return super.blockStoreDirectory() + "/nodeA";
            }

            @Override
            public int servicePort() {
                return 8090;
            }

            @Override
            public int targetPeerCount() {
                return 1;
            }
        }) {
            @Override
            protected Consensus createConsensus() {
                return mockConsensus;
            }
        };

        // Mine 100 blocks to create valid output for new transaction
        BigInteger privateKey = Simulation.randomPrivateKey();
        PublicKey publicKey = stateA.getConsensus()
            .getCurve()
            .getPublicKeyFromPrivateKey(privateKey);

        Miner minerA = stateA.getMiner();

        IndexedBlock previousBlock = stateA.getBlockchain()
            .getMainChain()
            .getGenesisBlock();
        Hash coinbaseOutputAddress = publicKey.getHash();

        for (int i = 0; i < mockConsensus.getCoinbaseMaturityDepth() + 1; i++) {
            Block newBlock = minerA.mineNewBlock(previousBlock, coinbaseOutputAddress);
            stateA.getBlockProcessor().processNewBlock(newBlock);

            previousBlock = stateA.getBlockchain().getIndexedBlock(newBlock.getHash());
            coinbaseOutputAddress = Simulation.randomHash();
        }

        IndexedBlock afterGenesisIndexed = stateA.getBlockchain()
            .getMainChain()
            .getBlockAtHeight(1);
        assertNotNull(afterGenesisIndexed);
        Block afterGenesis = stateA.getBlockchain().getBlock(afterGenesisIndexed);

        UnsignedTransaction utx = new UnsignedTransaction(
            Collections.singletonList(new Input(
                afterGenesis.getCoinbaseTransaction().getHash(),
                0
            )),
            Collections.singletonList(
                new Output(Simulation.randomHash(), mockConsensus.getBlockReward() - 1)
            )
        );

        Transaction tx = utx.sign(
            Collections.singletonList(stateA.getSigner().signMessage(
                utx.getSignableTransactionData(), privateKey
            ))
        );

        State stateB = new TestState(new MockBraboConfig(defaultConfig) {
            @Override
            public String blockStoreDirectory() {
                return super.blockStoreDirectory() + "/nodeB";
            }

            @Override
            public List<String> bootstrapPeers() {
                return Collections.singletonList(
                    "localhost:8090"
                );
            }

            @Override
            public int servicePort() {
                return 8091;
            }
        }) {
            @Override
            protected Consensus createConsensus() {
                return mockConsensus;
            }
        };

        State stateC = new TestState(new MockBraboConfig(defaultConfig) {
            @Override
            public String blockStoreDirectory() {
                return super.blockStoreDirectory() + "/nodeC";
            }

            @Override
            public List<String> bootstrapPeers() {
                return Collections.singletonList(
                    "localhost:8091"
                );
            }

            @Override
            public int servicePort() {
                return 8092;
            }

            @Override
            public int targetPeerCount() {
                return 1;
            }
        }) {
            @Override
            protected Consensus createConsensus() {
                return mockConsensus;
            }
        };

        // Start nodes
        stateA.getNode().start();
        stateB.getNode().start();
        await().atMost(30, TimeUnit.SECONDS)
            .until(() -> stateB.getBlockchain().getMainChain().getHeight() == stateA.getBlockchain()
                .getMainChain()
                .getHeight());
        stateC.getNode().start();
        await().atMost(30, TimeUnit.SECONDS)
            .until(() -> stateC.getBlockchain().getMainChain().getHeight() == stateA.getBlockchain()
                .getMainChain()
                .getHeight());


        stateA.getTransactionProcessor().processNewTransaction(tx);

        stateA.getEnvironment().announceTransactionRequest(stateA.getEnvironment()
            .getTransaction(tx.getHash()));

        await().atMost(30, TimeUnit.SECONDS)
            .until(() -> stateC.getEnvironment().getTransactionHashSet().contains
                (tx.getHash()));

        assertNotNull(stateC.getEnvironment().getTransaction(tx.getHash()));
        assertEquals(1, stateC.getEnvironment().getTransactionHashSet().size());

        stateA.getNode().stopAndBlock();
        stateB.getNode().stopAndBlock();
        stateC.getNode().stopAndBlock();
    }

        /**
         * <em>Setup:</em>
         * Three nodes: A, B and C.
         * C has a transaction X unknown to A and B.
         * A and B's transaction pools are empty.
         *
         * <em>Expected result:</em>
         * After announcing the transaction to B, B should eventually fetch the transaction
     from C and put it in in the transaction pool.
         * B should then announce the transaction to A, which should in turn fetch the
     transaction from B and add it to its transaction pool.
         */
        @Test
        void announceTransactionPropagatedToPeer() throws DatabaseException, IOException,
     InterruptedException {
            // Create default node A
            State stateA = new TestState(new MockBraboConfig(defaultConfig) {
                @Override
                public String blockStoreDirectory() {
                    return super.blockStoreDirectory() + "/nodeA";
                }

                @Override
                public int servicePort() {
                    return 8090;
                }
            }) {
                @Override
                protected Consensus createConsensus() {
                    return mockConsensus;
                }
            };

            // Mine 100 blocks to create valid output for new transaction
            BigInteger privateKey = Simulation.randomPrivateKey();
            PublicKey publicKey = stateA.getConsensus()
                .getCurve()
                .getPublicKeyFromPrivateKey(privateKey);

            Miner minerA = stateA.getMiner();

            IndexedBlock previousBlock = stateA.getBlockchain()
                .getMainChain()
                .getGenesisBlock();
            Hash coinbaseOutputAddress = publicKey.getHash();

            for (int i = 0; i < mockConsensus.getCoinbaseMaturityDepth() + 1; i++) {
                Block newBlock = minerA.mineNewBlock(previousBlock, coinbaseOutputAddress);
                stateA.getBlockProcessor().processNewBlock(newBlock);

                previousBlock = stateA.getBlockchain().getIndexedBlock(newBlock.getHash());
                coinbaseOutputAddress = Simulation.randomHash();
            }

            IndexedBlock afterGenesisIndexed = stateA.getBlockchain()
                .getMainChain()
                .getBlockAtHeight(1);
            assertNotNull(afterGenesisIndexed);
            Block afterGenesis = stateA.getBlockchain().getBlock(afterGenesisIndexed);

            UnsignedTransaction utx = new UnsignedTransaction(
                Collections.singletonList(new Input(
                    afterGenesis.getCoinbaseTransaction().getHash(),
                    0
                )),
                Collections.singletonList(
                    new Output(Simulation.randomHash(), mockConsensus.getBlockReward() - 1)
                )
            );

            Transaction tx = utx.sign(
                Collections.singletonList(stateA.getSigner().signMessage(
                    utx.getSignableTransactionData(), privateKey
                ))
            );

            State stateB = new TestState(new MockBraboConfig(defaultConfig) {
                @Override
                public String blockStoreDirectory() {
                    return super.blockStoreDirectory() + "/nodeB";
                }

                @Override
                public List<String> bootstrapPeers() {
                    return Collections.singletonList(
                        "localhost:8090"
                    );
                }

                @Override
                public int servicePort() {
                    return 8091;
                }
            }) {
                @Override
                protected Consensus createConsensus() {
                    return mockConsensus;
                }
            };

            State stateC = new TestState(new MockBraboConfig(defaultConfig) {
                @Override
                public String blockStoreDirectory() {
                    return super.blockStoreDirectory() + "/nodeC";
                }

                @Override
                public List<String> bootstrapPeers() {
                    return Collections.singletonList(
                        "localhost:8091"
                    );
                }

                @Override
                public int servicePort() {
                    return 8092;
                }
            }) {
                @Override
                protected Consensus createConsensus() {
                    return mockConsensus;
                }
            };

            // Start nodes
            stateA.getNode().start();
            stateB.getNode().start();
            await().atMost(30, TimeUnit.SECONDS)
                .until(() -> stateB.getBlockchain().getMainChain().getHeight() == stateA.getBlockchain()
                    .getMainChain()
                    .getHeight());

            stateC.getNode().start();
            await().atMost(30, TimeUnit.SECONDS)
                .until(() -> stateC.getBlockchain().getMainChain().getHeight() == stateA.getBlockchain()
                    .getMainChain()
                    .getHeight());

            stateA.getTransactionProcessor().processNewTransaction(tx);

            stateA.getEnvironment().announceTransactionRequest(stateA.getEnvironment()
     .getTransaction(tx.getHash()));

            await().atMost(30, TimeUnit.SECONDS)
                    .until(() -> stateC.getEnvironment().getTransactionHashSet().contains
     (tx.getHash()));

            assertNotNull(stateC.getEnvironment().getTransaction(tx.getHash()));
            assertEquals(1, stateC.getEnvironment().getTransactionHashSet().size());

            stateA.getNode().stopAndBlock();
            stateB.getNode().stopAndBlock();
            stateC.getNode().stopAndBlock();
        }

        /**
         * <em>Setup:</em>
         * Three nodes: A, B and C.
         * A has the longest blockchain.
         * B has the shortest blockchain.
         * C's blockchain length is between A's and B's blockchain length and has a fork with
     respect to A's blockchain.
         *
         * <em>Expected result:</em>
         * A, B and C's blockchains are identical and equal to A's blockchain.
         */
        @Test
        void updateBlockchain() throws DatabaseException, IOException, InterruptedException {

            // Create default node A
            State stateA = new TestState(new MockBraboConfig(defaultConfig) {
                @Override
                public String blockStoreDirectory() {
                    return super.blockStoreDirectory() + "/nodeA";
                }

                @Override
                public int servicePort() {
                    return 8090;
                }
            }) {
                @Override
                protected Consensus createConsensus() {
                    return mockConsensus;
                }
            };

            Miner minerA = stateA.getMiner();

            IndexedBlock previousBlockA = stateA.getBlockchain()
                .getMainChain()
                .getGenesisBlock();

            for (int i = 0; i < 20; i++) {
                Block newBlock = minerA.mineNewBlock(previousBlockA, Simulation.randomHash());
                stateA.getBlockProcessor().processNewBlock(newBlock);
                previousBlockA = stateA.getBlockchain().getIndexedBlock(newBlock.getHash());
            }

            State stateB = new TestState(new MockBraboConfig(defaultConfig) {
                @Override
                public String blockStoreDirectory() {
                    return super.blockStoreDirectory() + "/nodeB";
                }

                @Override
                public List<String> bootstrapPeers() {
                    return Collections.singletonList(
                        "localhost:8090"
                    );
                }

                @Override
                public int servicePort() {
                    return 8091;
                }
            }) {
                @Override
                protected Consensus createConsensus() {
                    return mockConsensus;
                }
            };

            for (int i = 1; i < 9; i++) {
                Block newBlock = stateA.getBlockchain().getBlock(stateA.getBlockchain().getMainChain().getBlockAtHeight(i));
                stateB.getBlockProcessor().processNewBlock(newBlock);
            }

            State stateC = new TestState(new MockBraboConfig(defaultConfig) {
                @Override
                public String blockStoreDirectory() {
                    return super.blockStoreDirectory() + "/nodeC";
                }

                @Override
                public List<String> bootstrapPeers() {
                    return Collections.singletonList(
                        "localhost:8091"
                    );
                }

                @Override
                public int servicePort() {
                    return 8092;
                }
            }) {
                @Override
                protected Consensus createConsensus() {
                    return mockConsensus;
                }
            };

            for (int i = 1; i < 11; i++) {
                Block newBlock = stateA.getBlockchain().getBlock(stateA.getBlockchain().getMainChain().getBlockAtHeight(i));
                stateC.getBlockProcessor().processNewBlock(newBlock);
            }

            Miner minerC = stateC.getMiner();

            IndexedBlock previousBlockC = stateC.getBlockchain().getMainChain().getBlockAtHeight(10);

            for (int i = 0; i < 8; i++) {
                Block newBlock = minerC.mineNewBlock(previousBlockC, Simulation.randomHash());
                stateC.getBlockProcessor().processNewBlock(newBlock);
                previousBlockC = stateC.getBlockchain().getIndexedBlock(newBlock.getHash());
            }

            // Start nodes
            stateA.getNode().start();
            stateB.getNode().start();
            await().atMost(30, TimeUnit.SECONDS)
                .until(() -> stateB.getBlockchain().getMainChain().getHeight() == stateA.getBlockchain()
                    .getMainChain()
                    .getHeight());

            stateC.getNode().start();
            await().atMost(30, TimeUnit.SECONDS)
                .until(() -> stateC.getBlockchain().getMainChain().getHeight() == stateA.getBlockchain()
                    .getMainChain()
                    .getHeight());


            await().atMost(30, TimeUnit.SECONDS)
                    .until(() -> stateA.getEnvironment().getTopBlockHeight() == stateC.getEnvironment().getTopBlockHeight() &&
                            stateA.getEnvironment().getTopBlockHeight() == stateB.getEnvironment
     ().getTopBlockHeight()
                    );

            assertEquals(stateA.getEnvironment().getTopBlockHeight(), stateB.getEnvironment()
     .getTopBlockHeight());
            assertEquals(stateA.getEnvironment().getTopBlockHeight(), stateC.getEnvironment()
     .getTopBlockHeight());
            assertEquals(stateA.getBlockchain().getMainChain().getTopBlock().getHash(),
                    stateC.getBlockchain().getMainChain().getTopBlock().getHash());
            assertEquals(stateA.getBlockchain().getMainChain().getTopBlock().getHash(),
                    stateB.getBlockchain().getMainChain().getTopBlock().getHash());

            stateA.getNode().stopAndBlock();
            stateB.getNode().stopAndBlock();
            stateC.getNode().stopAndBlock();
        }
//
//        /**
//         * <em>Setup:</em>
//         * Two nodes A and B.
//         * B's blockchain is in sync with A's blockchain.
//         * A also has a fork that is one blockheight less than the main chain.
//         *
//         *
//         * <em>Expected result:</em>
//         * When we mine two blocks on top of the fork, only announcing the second block, B
//     should sync with this fork, requesting both blocks.
//         * Then, after announcing another two blocks on the other fork (what used to be the
//     main chain), B should again switch the main chain.
//         */
//        @Test
//        void forkSwitching() throws DatabaseException, IOException, InterruptedException {
//            // Create a blockchain with a block mined on top of genesis block.
//            Consensus consensus = new Consensus();
//            List<Block> chainA = Simulation.randomBlockChainGenerator(20, consensus
//     .getGenesisBlock().getHash(), 1, 0, 5);
//            Block mainChainTopBlock = chainA.get(chainA.size() - 1);
//
//            // Create default node A
//            State stateA = new TestState(new MockBraboConfig(defaultConfig) {
//                @Override
//                public String blockStoreDirectory() {
//                    return super.blockStoreDirectory() + "/nodeA";
//                }
//
//                @Override
//                public int servicePort() {
//                    return 8090;
//                }
//            }) {
//                @Override
//                protected Consensus createConsensus() {
//                    return mockConsensus;
//                }
//            };
//
//            State stateB = new TestState(new MockBraboConfig(defaultConfig) {
//                @Override
//                public String blockStoreDirectory() {
//                    return super.blockStoreDirectory() + "/nodeB";
//                }
//
//                @Override
//                public List<String> bootstrapPeers() {
//                    return Collections.singletonList(
//                        "localhost:8090"
//                    );
//                }
//
//                @Override
//                public int servicePort() {
//                    return 8091;
//                }
//            }) {
//                @Override
//                protected Consensus createConsensus() {
//                    return mockConsensus;
//                }
//            };
//
//
//            Block forkMatchingBlock = chainA.get(10);
//            List<Block> chainAfork = Simulation.randomBlockChainGenerator(8, forkMatchingBlock
//     .getHash(), forkMatchingBlock.getBlockHeight() + 1, 0, 5);
//
//            Block forkTopBlock = chainAfork.get(chainAfork.size() - 1);
//
//            chainA.addAll(chainAfork);
//
//
//            nodeA.start();
//            nodeB.start();
//
//            assertEquals(nodeA.getEnvironment().getTopBlockHeight(), nodeB.getEnvironment()
//     .getTopBlockHeight());
//
//            // Mine two block on top of fork of A
//            Block forkBlock1 = Simulation.randomBlock(
//                    forkTopBlock.getHash(),
//                    forkTopBlock.getBlockHeight() + 1,
//                    0, 5, 20
//            );
//            Block forkBlock2 = Simulation.randomBlock(
//                    forkBlock1.getHash(),
//                    forkBlock1.getBlockHeight() + 1,
//                    0, 5, 20
//            );
//
//            blockProcessor.processNewBlock(forkBlock1);
//            blockProcessor.processNewBlock(forkBlock2);
//
//            assert forkBlock2.getBlockHeight() == mainChainTopBlock.getBlockHeight() + 1;
//
//            nodeA.getEnvironment().announceBlockRequest(forkBlock2);
//
//            await().atMost(30, TimeUnit.SECONDS)
//                    .until(() -> nodeA.getEnvironment().getTopBlockHeight() == nodeB
//     .getEnvironment().getTopBlockHeight());
//
//            assertEquals(
//                    nodeA.getEnvironment().getBlocksAbove(forkBlock1.getHash()).get(0),
//                    nodeB.getEnvironment().getBlocksAbove(forkBlock1.getHash()).get(0));
//
//            // Mine two block on top of new fork of A, what used to be the main chain
//            Block fork2Block1 = Simulation.randomBlock(
//                    mainChainTopBlock.getHash(),
//                    mainChainTopBlock.getBlockHeight() + 1,
//                    0, 5, 20
//            );
//            Block fork2Block2 = Simulation.randomBlock(
//                    fork2Block1.getHash(),
//                    fork2Block1.getBlockHeight() + 1,
//                    0, 5, 20
//            );
//
//            blockProcessor.processNewBlock(fork2Block1);
//            blockProcessor.processNewBlock(fork2Block2);
//
//            assert fork2Block2.getBlockHeight() > forkBlock2.getBlockHeight();
//
//            nodeA.getEnvironment().announceBlockRequest(fork2Block2);
//
//            await().atMost(30, TimeUnit.SECONDS)
//                    .until(() -> nodeA.getEnvironment().getTopBlockHeight() == nodeB
//     .getEnvironment().getTopBlockHeight());
//
//            assertEquals(
//                    nodeA.getEnvironment().getBlocksAbove(fork2Block1.getHash()).get(0),
//                    nodeB.getEnvironment().getBlocksAbove(fork2Block1.getHash()).get(0));
//
//
//            nodeA.stopAndBlock();
//            nodeB.stopAndBlock();
//        }
    //
    //    /**
    //     * <em>Setup:</em>
    //     * Three nodes A, B and C.
    //     * B's blockchain is in sync with A's blockchain.
    //     * C's blockchain is empty and only consists of the genesis block and only has peer B.
    //     * A also has a fork that is one blockheight less than the main chain.
    //     *
    //     *
    //     * <em>Expected result:</em>
    //     * When we mine two blocks on top of the fork, only announcing the second block, B
    // should sync with this fork, requesting both blocks.
    //     * Then, after announcing another two blocks on the other fork (what used to be the
    // main chain), B should again switch the main chain.
    //     */
    //    @Test
    //    @Disabled("Fails because transaction rules are not yet implemented.")
    //    void forkSwitchingPropagated() throws DatabaseException, IOException,
    // InterruptedException {
    //        // Create a blockchain with a block mined on top of genesis block.
    //        Consensus consensus = new Consensus();
    //        List<Block> chainA = Simulation.randomBlockChainGenerator(20, consensus
    // .getGenesisBlock().getHash(), 1, 0, 5);
    //        Block mainChainTopBlock = chainA.get(chainA.size() - 1);
    //
    //        Node nodeB = generateNodeWithBlocks(8091, new MockBraboConfig(defaultConfig) {
    //            @Override
    //            public String blockStoreDirectory() {
    //                return super.blockStoreDirectory() + "/nodeB";
    //            }
    //
    //            @Override
    //            public List<String> bootstrapPeers() {
    //                return Collections.singletonList("localhost:8090");
    //            }
    //        }, new Consensus(), chainA);
    //
    //
    //        Block forkMatchingBlock = chainA.get(10);
    //        List<Block> chainAfork = Simulation.randomBlockChainGenerator(8, forkMatchingBlock
    // .getHash(), forkMatchingBlock.getBlockHeight() + 1, 0, 5);
    //
    //        Block forkTopBlock = chainAfork.get(chainAfork.size() - 1);
    //
    //        assert forkTopBlock.getBlockHeight() == mainChainTopBlock.getBlockHeight() - 1;
    //
    //        chainA.addAll(chainAfork);
    //        Map.Entry<Node, BlockProcessor> fullNodeA = generateNodeAndProcessorWithBlocks
    // (8090, new MockBraboConfig(defaultConfig) {
    //            @Override
    //            public String blockStoreDirectory() {
    //                return super.blockStoreDirectory() + "/nodeA";
    //            }
    //
    //            @Override
    //            public int targetPeerCount() {
    //                return 1;
    //            }
    //        }, new Consensus(), chainA);
    //
    //        Node nodeC = generateNode(8092, new MockBraboConfig(defaultConfig) {
    //            @Override
    //            public String blockStoreDirectory() {
    //                return super.blockStoreDirectory() + "/nodeC";
    //            }
    //
    //            @Override
    //            public List<String> bootstrapPeers() {
    //                return Collections.singletonList("localhost:8091");
    //            }
    //
    //            @Override
    //            public int targetPeerCount() {
    //                return 1;
    //            }
    //        });
    //
    //        Node nodeA = fullNodeA.getKey();
    //        BlockProcessor blockProcessor = fullNodeA.getValue();
    //
    //        nodeA.start();
    //        nodeB.start();
    //        nodeC.start();
    //
    //        assertEquals(nodeA.getEnvironment().getTopBlockHeight(), nodeB.getEnvironment()
    // .getTopBlockHeight());
    //
    //        // Mine two block on top of fork of A
    //        Block forkBlock1 = Simulation.randomBlock(
    //                forkTopBlock.getHash(),
    //                forkTopBlock.getBlockHeight() + 1,
    //                0, 5, 20
    //        );
    //        Block forkBlock2 = Simulation.randomBlock(
    //                forkBlock1.getHash(),
    //                forkBlock1.getBlockHeight() + 1,
    //                0, 5, 20
    //        );
    //
    //        blockProcessor.processNewBlock(forkBlock1);
    //        blockProcessor.processNewBlock(forkBlock2);
    //
    //        assert forkBlock2.getBlockHeight() == mainChainTopBlock.getBlockHeight() + 1;
    //
    //        nodeA.getEnvironment().announceBlockRequest(forkBlock2);
    //
    //        await().atMost(30, TimeUnit.SECONDS)
    //                .until(() -> nodeA.getEnvironment().getTopBlockHeight() == nodeC
    // .getEnvironment().getTopBlockHeight());
    //
    //        assertEquals(
    //                nodeA.getEnvironment().getBlocksAbove(forkBlock1.getHash()).get(0),
    //                nodeC.getEnvironment().getBlocksAbove(forkBlock1.getHash()).get(0));
    //
    //        // Mine two block on top of new fork of A, what used to be the main chain
    //        Block fork2Block1 = Simulation.randomBlock(
    //                mainChainTopBlock.getHash(),
    //                mainChainTopBlock.getBlockHeight() + 1,
    //                0, 5, 20
    //        );
    //        Block fork2Block2 = Simulation.randomBlock(
    //                fork2Block1.getHash(),
    //                fork2Block1.getBlockHeight() + 1,
    //                0, 5, 20
    //        );
    //
    //        blockProcessor.processNewBlock(fork2Block1);
    //        blockProcessor.processNewBlock(fork2Block2);
    //
    //        assert fork2Block2.getBlockHeight() > forkBlock2.getBlockHeight();
    //
    //        nodeA.getEnvironment().announceBlockRequest(fork2Block2);
    //
    //        await().atMost(30, TimeUnit.SECONDS)
    //                .until(() -> nodeA.getEnvironment().getTopBlockHeight() == nodeC
    // .getEnvironment().getTopBlockHeight());
    //
    //        assertEquals(
    //                nodeA.getEnvironment().getBlocksAbove(fork2Block1.getHash()).get(0),
    //                nodeC.getEnvironment().getBlocksAbove(fork2Block1.getHash()).get(0));
    //
    //
    //        nodeA.stopAndBlock();
    //        nodeB.stopAndBlock();
    //        nodeC.stopAndBlock();
    //    }
    //
    //    /**
    //     * node B tries to acquire the transaction pool from its peer A on startup.
    //     */
    //    @Test
    //    void seekTransctionPool() throws DatabaseException, IOException, InterruptedException {
    //        List<Transaction> transactions = Simulation.repeatedBuilder(() -> Simulation
    // .randomTransaction(0, 5), 200);
    //
    //        Node nodeA = generateNodeWithTransactions(8090, new MockBraboConfig(defaultConfig) {
    //            @Override
    //            public String blockStoreDirectory() {
    //                return super.blockStoreDirectory() + "/nodeA";
    //            }
    //        }, new Consensus(), transactions);
    //
    //        Node nodeB = generateNode(8091, new MockBraboConfig(defaultConfig) {
    //            @Override
    //            public String blockStoreDirectory() {
    //                return super.blockStoreDirectory() + "/nodeB";
    //            }
    //
    //            @Override
    //            public List<String> bootstrapPeers() {
    //                return Collections.singletonList("localhost:8090");
    //            }
    //        });
    //
    //        // Start nodes
    //        nodeA.start();
    //        nodeB.start();
    //
    //        await().atMost(30, TimeUnit.SECONDS)
    //                .until(() -> nodeB.getEnvironment().getTransactionHashSet().size() ==
    // transactions.size());
    //
    //        nodeA.stopAndBlock();
    //        nodeB.stopAndBlock();
    //    }
}
