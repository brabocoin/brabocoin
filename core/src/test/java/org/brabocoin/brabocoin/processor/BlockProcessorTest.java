package org.brabocoin.brabocoin.processor;

import com.google.protobuf.ByteString;
import org.brabocoin.brabocoin.Constants;
import org.brabocoin.brabocoin.chain.Blockchain;
import org.brabocoin.brabocoin.crypto.EllipticCurve;
import org.brabocoin.brabocoin.crypto.Hashing;
import org.brabocoin.brabocoin.crypto.Signer;
import org.brabocoin.brabocoin.dal.*;
import org.brabocoin.brabocoin.exceptions.DatabaseException;
import org.brabocoin.brabocoin.model.*;
import org.brabocoin.brabocoin.node.config.BraboConfig;
import org.brabocoin.brabocoin.node.config.BraboConfigProvider;
import org.brabocoin.brabocoin.testutil.MockBraboConfig;
import org.brabocoin.brabocoin.testutil.Simulation;
import org.brabocoin.brabocoin.testutil.TestState;
import org.brabocoin.brabocoin.validation.Consensus;
import org.brabocoin.brabocoin.validation.ValidationStatus;
import org.brabocoin.brabocoin.validation.block.BlockValidationResult;
import org.brabocoin.brabocoin.validation.block.BlockValidator;
import org.brabocoin.brabocoin.validation.transaction.TransactionValidator;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.*;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test {@link BlockProcessor}.
 */
class BlockProcessorTest {

    private static final String BLOCK_FILE_LOCATION = "testenv/blocks";
    private static final @NotNull File blocksDirectory = new File(BLOCK_FILE_LOCATION);
    private static BraboConfig config;

    private TestState state;

    @BeforeAll
    static void loadConfig() {
        BraboConfig defaultConfig = BraboConfigProvider.getConfig().bind("brabo", BraboConfig.class);
        config = new MockBraboConfig(defaultConfig) {
            @Override
            public String blockStoreDirectory() {
                return BLOCK_FILE_LOCATION;
            }
        };
    }

    @AfterEach
    void tearDown() {
        // Remove block files
        for (File f : blocksDirectory.listFiles()) {
            f.delete();
        }
    }

    @BeforeEach
    void setUp() throws DatabaseException {
        state = new TestState(config) {
            @Override
            protected Consensus createConsensus() {
                return new Consensus() {
                    @Override
                    public @NotNull Hash getTargetValue() {
                        return Hashing.digestSHA256(ByteString.copyFromUtf8("easy"));
                    }
                };
            }
        };
    }

    @Test
    void invalidBlock() throws DatabaseException {
        Block block = Simulation.randomBlockChainGenerator(1).get(0);

        ValidationStatus status = state.getBlockProcessor().processNewBlock(block);
        assertEquals(ValidationStatus.INVALID, status);
    }

    @Test
    void addAsOrphanParentUnknown() throws DatabaseException {
        Block block = Simulation.randomOrphanBlock(
                state.getConsensus(),
                1,
                10,
                10,
                20
        );
        ValidationStatus status = state.getBlockProcessor().processNewBlock(block);

        assertEquals(ValidationStatus.ORPHAN, status);
        assertTrue(state.getBlockchain().isOrphan(block.getHash()));
    }

    @Test
    void addAsOrphanParentOrphan() throws DatabaseException {
        List<Block> blocks = Simulation.randomBlockChainGenerator(2);
        Block parent = blocks.get(0);
        Block child = blocks.get(1);

        state.getBlockchain().storeBlock(parent);
        state.getBlockchain().addOrphan(parent);

        ValidationStatus status = state.getBlockProcessor().processNewBlock(child);
        assertEquals(ValidationStatus.ORPHAN, status);
        assertTrue(state.getBlockchain().isOrphan(child.getHash()));
    }

    @Test
    void addCoinbaseToGenesis() throws DatabaseException {
        state = new TestState(config) {
            @Override
            protected BlockValidator createBlockValidator() {
                return new BlockValidator(
                        this
                ) {
                    @Override
                    public BlockValidationResult checkConnectBlockValid(@NotNull Block block) {
                        return BlockValidationResult.passed();
                    }

                    @Override
                    public BlockValidationResult checkIncomingBlockValid(@NotNull Block block) {
                        return BlockValidationResult.passed();
                    }

                    @Override
                    public BlockValidationResult checkPostOrphanBlockValid(@NotNull Block block) {
                        return BlockValidationResult.passed();
                    }
                };
            }
        };

        Output output = Simulation.randomOutput();
        Transaction transaction = new Transaction(
                Collections.emptyList(),
                Collections.singletonList(output), Collections.emptyList()
        );
        Block block = new Block(
                state.getConsensus().getGenesisBlock().getHash(),
                Simulation.randomHash(),
                Simulation.randomHash(),
                Simulation.randomBigInteger(), 1,
                Collections.singletonList(transaction)
        );
        Hash hash = block.getHash();
        Hash tHash = transaction.getHash();

        // Add to transaction pool manually
        state.getTransactionPool().addIndependentTransaction(transaction);
        state.getPoolUTXODatabase().setOutputsUnspent(transaction, Constants.TRANSACTION_POOL_HEIGHT);

        ValidationStatus status = state.getBlockProcessor().processNewBlock(block);

        // Check chain
        assertEquals(ValidationStatus.VALID, status);
        assertEquals(hash, state.getBlockchain().getMainChain().getTopBlock().getHash());
        assertEquals(1, state.getBlockchain().getMainChain().getHeight());

        // Check UTXO
        assertTrue(state.getChainUTXODatabase().isUnspent(tHash, 0));

        // Check transaction pool
        assertFalse(state.getTransactionPool().hasValidTransaction(tHash));
        assertFalse(state.getChainUTXODatabase().isUnspent(tHash, 0));

        // Check undo data
        assertNotNull(state.getBlockchain().findBlockUndo(hash));
    }

    @Test
    void connectNonCoinbase() throws DatabaseException {
        state = new TestState(config) {
            @Override
            protected BlockValidator createBlockValidator() {
                return new BlockValidator(
                        this
                ) {
                    @Override
                    public BlockValidationResult checkConnectBlockValid(@NotNull Block block) {
                        return BlockValidationResult.passed();
                    }

                    @Override
                    public BlockValidationResult checkIncomingBlockValid(@NotNull Block block) {
                        return BlockValidationResult.passed();
                    }

                    @Override
                    public BlockValidationResult checkPostOrphanBlockValid(@NotNull Block block) {
                        return BlockValidationResult.passed();
                    }
                };
            }
        };

        Output output = Simulation.randomOutput();
        Transaction transactionA = new Transaction(
                Collections.emptyList(),
                Collections.singletonList(output), Collections.emptyList()
        );
        Block blockA = new Block(
                state.getConsensus().getGenesisBlock().getHash(),
                Simulation.randomHash(),
                Simulation.randomHash(),
                Simulation.randomBigInteger(), 1,
                Collections.singletonList(transactionA)
        );
        Hash hashA = blockA.getHash();
        Hash tHashA = transactionA.getHash();

        Transaction transactionB = new Transaction(
                Collections.singletonList(new Input( tHashA, 0)),
                Collections.singletonList(Simulation.randomOutput()), Collections.emptyList()
        );
        Block blockB = new Block(
                hashA,
                Simulation.randomHash(),
                Simulation.randomHash(),
                Simulation.randomBigInteger(), 2,
                Collections.singletonList(transactionB)
        );
        Hash tHashB = transactionB.getHash();

        state.getBlockProcessor().processNewBlock(blockA);

        ValidationStatus status = state.getBlockProcessor().processNewBlock(blockB);

        // Check chain
        assertEquals(ValidationStatus.VALID, status);

        // Check UTXO
        assertTrue(state.getChainUTXODatabase().isUnspent(tHashB, 0));
        assertFalse(state.getChainUTXODatabase().isUnspent(tHashA, 0));
    }

    @Test
    void addAsFork() throws DatabaseException {
        state = new TestState(config) {
            @Override
            protected BlockValidator createBlockValidator() {
                return new BlockValidator(
                        this
                ) {
                    @Override
                    public BlockValidationResult checkConnectBlockValid(@NotNull Block block) {
                        return BlockValidationResult.passed();
                    }

                    @Override
                    public BlockValidationResult checkIncomingBlockValid(@NotNull Block block) {
                        return BlockValidationResult.passed();
                    }

                    @Override
                    public BlockValidationResult checkPostOrphanBlockValid(@NotNull Block block) {
                        return BlockValidationResult.passed();
                    }
                };
            }
        };

        // Main chain: genesis - A - B
        // Fork:              \_ C
        Block blockA = new Block(
                state.getConsensus().getGenesisBlock().getHash(),
                Simulation.randomHash(),
                Simulation.randomHash(),
                Simulation.randomBigInteger(), 1,
                Collections.singletonList(new Transaction(
                        Collections.emptyList(),
                        Collections.singletonList(Simulation.randomOutput()), Collections.emptyList()
                ))
        );
        Hash hashA = blockA.getHash();

        Block blockB = new Block(
                hashA,
                Simulation.randomHash(),
                Simulation.randomHash(),
                Simulation.randomBigInteger(), 2,
                Collections.singletonList(new Transaction(
                        Collections.emptyList(),
                        Collections.singletonList(Simulation.randomOutput()), Collections.emptyList()
                ))
        );
        Hash hashB = blockB.getHash();

        Transaction transaction = new Transaction(
                Collections.emptyList(),
                Collections.singletonList(Simulation.randomOutput()), Collections.emptyList()
        );
        Hash tHash = transaction.getHash();

        Block blockC = new Block(
                state.getConsensus().getGenesisBlock().getHash(),
                Simulation.randomHash(),
                Simulation.randomHash(),
                Simulation.randomBigInteger(), 1,
                Collections.singletonList(transaction)
        );
        Hash hashC = blockC.getHash();

        // Add to transaction pool manually
        state.getTransactionPool().addIndependentTransaction(transaction);
        state.getPoolUTXODatabase().setOutputsUnspent(transaction, Constants.TRANSACTION_POOL_HEIGHT);

        state.getBlockProcessor().processNewBlock(blockA);
        state.getBlockProcessor().processNewBlock(blockB);

        ValidationStatus status = state.getBlockProcessor().processNewBlock(blockC);

        // Check chain
        assertEquals(ValidationStatus.VALID, status);
        assertEquals(hashB, state.getBlockchain().getMainChain().getTopBlock().getHash());
        assertEquals(2, state.getBlockchain().getMainChain().getHeight());
        assertTrue(state.getBlockchain().isBlockStored(hashC));

        // Check UTXO
        assertFalse(state.getChainUTXODatabase().isUnspent(tHash, 0));

        // Check transaction pool
        assertTrue(state.getTransactionPool().hasValidTransaction(tHash));
        assertTrue(state.getPoolUTXODatabase().isUnspent(tHash, 0));

        // Check undo data
        assertNull(state.getBlockchain().findBlockUndo(hashC));
    }

    @Test
    void addAsForkWithOrphansSwitch() throws DatabaseException {
        // Main chain: genesis - A - B - C
        // Fork:              \_ D - E - F - G

        Block blockA = new Block(
                state.getConsensus().getGenesisBlock().getHash(),
                Simulation.randomHash(),
                Simulation.randomHash(),
                Simulation.randomBigInteger(), 1,
                Collections.singletonList(new Transaction(
                        Collections.emptyList(),
                        Collections.singletonList(Simulation.randomOutput()), Collections.emptyList()
                ))
        );
        Hash hashA = blockA.getHash();

        Block blockB = new Block(
                hashA,
                Simulation.randomHash(),
                Simulation.randomHash(),
                Simulation.randomBigInteger(), 2,
                Collections.singletonList(new Transaction(
                        Collections.emptyList(),
                        Collections.singletonList(Simulation.randomOutput()), Collections.emptyList()
                ))
        );
        Hash hashB = blockB.getHash();

        Block blockC = new Block(
                hashB,
                Simulation.randomHash(),
                Simulation.randomHash(),
                Simulation.randomBigInteger(), 3,
                Collections.singletonList(new Transaction(
                        Collections.emptyList(),
                        Collections.singletonList(Simulation.randomOutput()), Collections.emptyList()
                ))
        );
        Hash hashC = blockC.getHash();

        Block blockD = new Block(
                state.getConsensus().getGenesisBlock().getHash(),
                Simulation.randomHash(),
                Simulation.randomHash(),
                Simulation.randomBigInteger(), 1,
                Collections.singletonList(new Transaction(
                        Collections.emptyList(),
                        Collections.singletonList(Simulation.randomOutput()), Collections.emptyList()
                ))
        );
        Hash hashD = blockD.getHash();

        Block blockE = new Block(
                hashD,
                Simulation.randomHash(),
                Simulation.randomHash(),
                Simulation.randomBigInteger(), 2,
                Collections.singletonList(new Transaction(
                        Collections.emptyList(),
                        Collections.singletonList(Simulation.randomOutput()), Collections.emptyList()
                ))
        );
        Hash hashE = blockE.getHash();

        Block blockF = new Block(
                hashE,
                Simulation.randomHash(),
                Simulation.randomHash(),
                Simulation.randomBigInteger(), 3,
                Collections.singletonList(new Transaction(
                        Collections.emptyList(),
                        Collections.singletonList(Simulation.randomOutput()), Collections.emptyList()
                ))
        );
        Hash hashF = blockF.getHash();


        Block blockG = new Block(
                hashF,
                Simulation.randomHash(),
                Simulation.randomHash(),
                Simulation.randomBigInteger(), 4,
                Collections.singletonList(new Transaction(
                        Collections.emptyList(),
                        Collections.singletonList(Simulation.randomOutput()), Collections.emptyList()
                ))
        );
        Hash hashG = blockG.getHash();
        state.getBlockProcessor().processNewBlock(blockA);
        state.getBlockProcessor().processNewBlock(blockB);
        state.getBlockProcessor().processNewBlock(blockC);
        state.getBlockProcessor().processNewBlock(blockD);
        state.getBlockProcessor().processNewBlock(blockF);
        state.getBlockProcessor().processNewBlock(blockG);

        ValidationStatus status = state.getBlockProcessor().processNewBlock(blockE);

        // Check chain
        assertEquals(ValidationStatus.VALID, status);
        assertEquals(hashG, state.getBlockchain().getMainChain().getTopBlock().getHash());
        assertEquals(4, state.getBlockchain().getMainChain().getHeight());

        assertTrue(state.getBlockchain().getMainChain().contains(state.getBlockchain().getIndexedBlock(hashD)));
        assertTrue(state.getBlockchain().getMainChain().contains(state.getBlockchain().getIndexedBlock(hashE)));
        assertTrue(state.getBlockchain().getMainChain().contains(state.getBlockchain().getIndexedBlock(hashF)));

        assertFalse(state.getBlockchain().getMainChain().contains(state.getBlockchain().getIndexedBlock(hashA)));
        assertFalse(state.getBlockchain().getMainChain().contains(state.getBlockchain().getIndexedBlock(hashB)));
        assertFalse(state.getBlockchain().getMainChain().contains(state.getBlockchain().getIndexedBlock(hashC)));

        // Check UTXO
        assertFalse(state.getChainUTXODatabase().isUnspent(blockA.getTransactions().get(0).getHash(), 0));
        assertFalse(state.getChainUTXODatabase().isUnspent(blockB.getTransactions().get(0).getHash(), 0));
        assertFalse(state.getChainUTXODatabase().isUnspent(blockC.getTransactions().get(0).getHash(), 0));

        assertTrue(state.getChainUTXODatabase().isUnspent(blockD.getTransactions().get(0).getHash(), 0));
        assertTrue(state.getChainUTXODatabase().isUnspent(blockE.getTransactions().get(0).getHash(), 0));
        assertTrue(state.getChainUTXODatabase().isUnspent(blockF.getTransactions().get(0).getHash(), 0));
        assertTrue(state.getChainUTXODatabase().isUnspent(blockG.getTransactions().get(0).getHash(), 0));

        // Check transaction pool
        assertTrue(state.getTransactionPool().hasValidTransaction(blockA.getTransactions().get(0).getHash()));
        assertTrue(state.getTransactionPool().hasValidTransaction(blockB.getTransactions().get(0).getHash()));
        assertTrue(state.getTransactionPool().hasValidTransaction(blockC.getTransactions().get(0).getHash()));
    }

}
