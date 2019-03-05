package org.brabocoin.brabocoin.processor;

import org.brabocoin.brabocoin.Constants;
import org.brabocoin.brabocoin.chain.IndexedBlock;
import org.brabocoin.brabocoin.config.BraboConfig;
import org.brabocoin.brabocoin.config.MutableBraboConfig;
import org.brabocoin.brabocoin.crypto.PublicKey;
import org.brabocoin.brabocoin.exceptions.DatabaseException;
import org.brabocoin.brabocoin.exceptions.DestructionException;
import org.brabocoin.brabocoin.model.Block;
import org.brabocoin.brabocoin.model.Hash;
import org.brabocoin.brabocoin.model.Input;
import org.brabocoin.brabocoin.model.Output;
import org.brabocoin.brabocoin.model.Transaction;
import org.brabocoin.brabocoin.model.UnsignedTransaction;
import org.brabocoin.brabocoin.model.dal.UnspentOutputInfo;
import org.brabocoin.brabocoin.testutil.LegacyBraboConfig;
import org.brabocoin.brabocoin.testutil.MockLegacyConfig;
import org.brabocoin.brabocoin.testutil.Simulation;
import org.brabocoin.brabocoin.testutil.TestState;
import org.brabocoin.brabocoin.validation.ValidationStatus;
import org.brabocoin.brabocoin.validation.block.BlockValidationResult;
import org.brabocoin.brabocoin.validation.block.BlockValidator;
import org.brabocoin.brabocoin.validation.block.rules.KnownParentBlkRule;
import org.brabocoin.brabocoin.validation.consensus.Consensus;
import org.brabocoin.brabocoin.validation.rule.RuleBookFailMarker;
import org.brabocoin.brabocoin.validation.rule.RuleList;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test {@link BlockProcessor}.
 */
class BlockProcessorTest {

    private static final String walletPath = "src/test/resources/data/wallet/wallet.dat";
    private static final String txHistPath = "src/test/resources/data/wallet/txhist.dat";
    private static final File walletFile = new File(walletPath);
    private static final File txhistFile = new File(txHistPath);

    private static final String BLOCK_FILE_LOCATION = "testenv/blocks";
    private static final @NotNull File blocksDirectory = new File(BLOCK_FILE_LOCATION);
    private static MockLegacyConfig config;

    private TestState state;

    @BeforeAll
    static void loadConfig() {
        BraboConfig defaultConfig = new MutableBraboConfig();
        config = new MockLegacyConfig(new LegacyBraboConfig(defaultConfig)) {
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
        if (walletFile.exists()) {
            walletFile.delete();
        }

        if (txhistFile.exists()) {
            txhistFile.delete();
        }

        state = new TestState(config) {
            @Override
            protected BlockValidator createBlockValidator() {
                return new BlockValidator(
                    this
                ) {
                    @Override
                    public BlockValidationResult validate(@NotNull Block block,
                                                          @NotNull RuleList ruleList) {
                        return BlockValidationResult.passed();
                    }
                };
            }
        };
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
                    public BlockValidationResult validate(@NotNull Block block,
                                                          @NotNull RuleList ruleList) {
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
            Collections.singletonList(transaction),
            0
        );
        Hash hash = block.getHash();
        Hash tHash = transaction.getHash();

        // Add to transaction pool manually
        state.getTransactionPool().addIndependentTransaction(transaction);
        state.getPoolUTXODatabase()
            .setOutputsUnspent(transaction, Constants.TRANSACTION_POOL_HEIGHT);

        ValidationStatus status = state.getBlockProcessor().processNewBlock(block, false).getStatus();

        // Check chain
        assertEquals(ValidationStatus.VALID, status);
        assertEquals(hash, state.getBlockchain().getMainChain().getTopBlock().getHash());
        assertEquals(1, state.getBlockchain().getMainChain().getHeight());

        // Check UTXO
        assertTrue(state.getChainUTXODatabase().isUnspent(tHash, 0));

        // Check transaction pool
        assertFalse(state.getTransactionPool().hasValidTransaction(tHash));
        assertFalse(state.getPoolUTXODatabase().isUnspent(tHash, 0));

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
                    public BlockValidationResult validate(@NotNull Block block,
                                                          @NotNull RuleList ruleList) {
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
            Collections.singletonList(transactionA),
            0
        );
        Hash hashA = blockA.getHash();
        Hash tHashA = transactionA.getHash();

        Transaction transactionB = new Transaction(
            Collections.singletonList(new Input(tHashA, 0)),
            Collections.singletonList(Simulation.randomOutput()), Collections.emptyList()
        );
        Block blockB = new Block(
            hashA,
            Simulation.randomHash(),
            Simulation.randomHash(),
            Simulation.randomBigInteger(), 2,
            Collections.singletonList(transactionB),
            0
        );
        Hash tHashB = transactionB.getHash();

        state.getBlockProcessor().processNewBlock(blockA, false);

        ValidationStatus status = state.getBlockProcessor().processNewBlock(blockB, false).getStatus();

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
                    public BlockValidationResult validate(@NotNull Block block,
                                                          @NotNull RuleList ruleList) {
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
            Collections.singletonList(Transaction.coinbase(Simulation.randomOutput(), 1)),
            0
        );
        Hash hashA = blockA.getHash();

        Block blockB = new Block(
            hashA,
            Simulation.randomHash(),
            Simulation.randomHash(),
            Simulation.randomBigInteger(), 2,
            Collections.singletonList(Transaction.coinbase(Simulation.randomOutput(), 2)),
            0
        );
        Hash hashB = blockB.getHash();

        Transaction transaction = Transaction.coinbase(Simulation.randomOutput(), 1);
        Hash tHash = transaction.getHash();

        Block blockC = new Block(
            state.getConsensus().getGenesisBlock().getHash(),
            Simulation.randomHash(),
            Simulation.randomHash(),
            Simulation.randomBigInteger(), 1,
            Collections.singletonList(transaction),
            0
        );
        Hash hashC = blockC.getHash();

        // Add to transaction pool manually
        state.getTransactionPool().addIndependentTransaction(transaction);
        state.getPoolUTXODatabase()
            .setOutputsUnspent(transaction, Constants.TRANSACTION_POOL_HEIGHT);

        state.getBlockProcessor().processNewBlock(blockA, false);
        state.getBlockProcessor().processNewBlock(blockB, false);

        ValidationStatus status = state.getBlockProcessor().processNewBlock(blockC, false).getStatus();

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

        Transaction transaction = new Transaction(
            Collections.singletonList(Simulation.randomInput()),
            Collections.singletonList(Simulation.randomOutput()), Collections.emptyList()
        );

        Block blockA = new Block(
            state.getConsensus().getGenesisBlock().getHash(),
            Simulation.randomHash(),
            Simulation.randomHash(),
            Simulation.randomBigInteger(), 1,
            Arrays.asList(Transaction.coinbase(Simulation.randomOutput(), 1), transaction),
            0
        );
        Hash hashA = blockA.getHash();

        Block blockB = new Block(
            hashA,
            Simulation.randomHash(),
            Simulation.randomHash(),
            Simulation.randomBigInteger(), 2,
            Collections.singletonList(Transaction.coinbase(Simulation.randomOutput(), 2)),
            0
        );
        Hash hashB = blockB.getHash();

        Block blockC = new Block(
            hashB,
            Simulation.randomHash(),
            Simulation.randomHash(),
            Simulation.randomBigInteger(), 3,
            Collections.singletonList(Transaction.coinbase(Simulation.randomOutput(), 3)),
            0
        );
        Hash hashC = blockC.getHash();

        Block blockD = new Block(
            state.getConsensus().getGenesisBlock().getHash(),
            Simulation.randomHash(),
            Simulation.randomHash(),
            Simulation.randomBigInteger(), 1,
            Collections.singletonList(Transaction.coinbase(Simulation.randomOutput(), 1)),
            0
        );
        Hash hashD = blockD.getHash();

        Block blockE = new Block(
            hashD,
            Simulation.randomHash(),
            Simulation.randomHash(),
            Simulation.randomBigInteger(), 2,
            Collections.singletonList(Transaction.coinbase(Simulation.randomOutput(), 2)),
            0
        );
        Hash hashE = blockE.getHash();

        Block blockF = new Block(
            hashE,
            Simulation.randomHash(),
            Simulation.randomHash(),
            Simulation.randomBigInteger(), 3,
            Collections.singletonList(Transaction.coinbase(Simulation.randomOutput(), 3)),
            0
        );
        Hash hashF = blockF.getHash();


        Block blockG = new Block(
            hashF,
            Simulation.randomHash(),
            Simulation.randomHash(),
            Simulation.randomBigInteger(), 4,
            Collections.singletonList(Transaction.coinbase(Simulation.randomOutput(), 4)),
            0
        );
        Hash hashG = blockG.getHash();

        state = new TestState(config) {
            @Override
            protected BlockValidator createBlockValidator() {
                return new BlockValidator(
                    this
                ) {
                    @Override
                    public BlockValidationResult validate(@NotNull Block block,
                                                          @NotNull RuleList ruleList) {
                        if (ruleList == BlockValidator.INCOMING_BLOCK) {
                            if (block.getHash().equals(blockF.getHash()) || block.getHash()
                                .equals(blockG.getHash())) {
                                return BlockValidationResult.failed(new RuleBookFailMarker(
                                    KnownParentBlkRule.class));
                            }
                        }
                        return BlockValidationResult.passed();
                    }
                };
            }
        };

        Input input = transaction.getInputs().get(0);
        state.getChainUTXODatabase()
            .addUnspentOutputInfo(input.getReferencedTransaction(),
                input.getReferencedOutputIndex(),
                new UnspentOutputInfo(false, 0, 10L, Simulation.randomHash())
            );

        state.getBlockProcessor().processNewBlock(blockA, false);
        state.getBlockProcessor().processNewBlock(blockB, false);
        state.getBlockProcessor().processNewBlock(blockC, false);
        state.getBlockProcessor().processNewBlock(blockD, false);
        state.getBlockProcessor().processNewBlock(blockF, false);
        state.getBlockProcessor().processNewBlock(blockG, false);

        ValidationStatus status = state.getBlockProcessor().processNewBlock(blockE, false).getStatus();

        // Check chain
        assertEquals(ValidationStatus.VALID, status);
        assertEquals(hashG, state.getBlockchain().getMainChain().getTopBlock().getHash());
        assertEquals(4, state.getBlockchain().getMainChain().getHeight());

        assertTrue(state.getBlockchain()
            .getMainChain()
            .contains(state.getBlockchain().getIndexedBlock(hashD)));
        assertTrue(state.getBlockchain()
            .getMainChain()
            .contains(state.getBlockchain().getIndexedBlock(hashE)));
        assertTrue(state.getBlockchain()
            .getMainChain()
            .contains(state.getBlockchain().getIndexedBlock(hashF)));

        assertFalse(state.getBlockchain()
            .getMainChain()
            .contains(state.getBlockchain().getIndexedBlock(hashA)));
        assertFalse(state.getBlockchain()
            .getMainChain()
            .contains(state.getBlockchain().getIndexedBlock(hashB)));
        assertFalse(state.getBlockchain()
            .getMainChain()
            .contains(state.getBlockchain().getIndexedBlock(hashC)));

        // Check UTXO
        assertFalse(state.getChainUTXODatabase()
            .isUnspent(blockA.getTransactions().get(1).getHash(), 0));
        assertFalse(state.getChainUTXODatabase()
            .isUnspent(blockB.getTransactions().get(0).getHash(), 0));
        assertFalse(state.getChainUTXODatabase()
            .isUnspent(blockC.getTransactions().get(0).getHash(), 0));

        assertTrue(state.getChainUTXODatabase()
            .isUnspent(blockD.getTransactions().get(0).getHash(), 0));
        assertTrue(state.getChainUTXODatabase()
            .isUnspent(blockE.getTransactions().get(0).getHash(), 0));
        assertTrue(state.getChainUTXODatabase()
            .isUnspent(blockF.getTransactions().get(0).getHash(), 0));
        assertTrue(state.getChainUTXODatabase()
            .isUnspent(blockG.getTransactions().get(0).getHash(), 0));

        // Check transaction pool
        assertTrue(state.getTransactionPool()
            .hasValidTransaction(blockA.getTransactions().get(1).getHash()));
        assertFalse(state.getTransactionPool()
            .hasValidTransaction(blockB.getTransactions().get(0).getHash()));
        assertFalse(state.getTransactionPool()
            .hasValidTransaction(blockC.getTransactions().get(0).getHash()));
    }

    /**
     * Given a chain Genesis-A-B-C-D where in block D a transaction T is mined that spends the
     * output of the coinbase transaction of block B.
     * Then, a fork switch on A is attempted, switching to a chain Genesis-A-E-F-G-H.
     *
     * The transaction T becomes orphan (since the coinbase of B is removed from the main chain).
     * This test tests whether T is not present in either chain and pool UTXO.
     */
    @Test
    void forkSwitchOrphanTxsRemovedFromUtxo() throws DatabaseException, DestructionException {
        Consensus mockConsensus = new Consensus() {
            @Override
            public int getCoinbaseMaturityDepth() {
                return 1;
            }
        };

        state = new TestState(config, mockConsensus);

        PublicKey key = state.getWallet().getPublicKeys().iterator().next();

        // Mine block A
        Block blockA = state.getMiner().mineNewBlock(
            state.getBlockchain().getMainChain().getTopBlock(),
            key.getHash()
        );
        assertNotNull(blockA);
        state.getBlockProcessor().processNewBlock(blockA, true);

        // Mine block B
        Block blockB = state.getMiner().mineNewBlock(
            state.getBlockchain().getMainChain().getTopBlock(),
            key.getHash()
        );
        assertNotNull(blockB);
        state.getBlockProcessor().processNewBlock(blockB, true);

        // Mine block C
        Block blockC = state.getMiner().mineNewBlock(
            state.getBlockchain().getMainChain().getTopBlock(),
            key.getHash()
        );
        assertNotNull(blockC);
        state.getBlockProcessor().processNewBlock(blockC, true);

        // Create a transaction that spends the output of block B
        UnsignedTransaction utx = new UnsignedTransaction(
            Collections.singletonList(new Input(blockB.getCoinbaseTransaction().getHash(), 0)),
            Collections.singletonList(new Output(key.getHash(), 9))
        );
        Transaction tx = state.getWallet().signTransaction(utx).getTransaction();
        state.getTransactionProcessor().processNewTransaction(tx);

        assertTrue(state.getPoolUTXODatabase().isUnspent(tx.getHash(), 0));

        // Mine block D with this transaction
        Block blockD = state.getMiner().mineNewBlock(
            state.getBlockchain().getMainChain().getTopBlock(),
            key.getHash()
        );
        assertNotNull(blockD);
        assertEquals(2, blockD.getTransactions().size());
        assertEquals(blockD.getTransactions().get(1).getHash(), tx.getHash());
        state.getBlockProcessor().processNewBlock(blockD, true);

        // Check that the tx is removed from the pool UTXO and added to the chain UTXO
        assertFalse(state.getPoolUTXODatabase().isUnspent(tx.getHash(), 0));
        assertTrue(state.getChainUTXODatabase().isUnspent(tx.getHash(), 0));

        // Now do a fork switch to Genesis-A-E-F-G-H
        IndexedBlock blk = state.getBlockchain().getIndexedBlock(blockA.getHash());
        for (int i = 0; i < 4; i++) {
            assertNotNull(blk);
            Block minedBlk = state.getMiner().mineNewBlock(blk, key.getHash());
            assertNotNull(minedBlk);
            state.getBlockProcessor().processNewBlock(minedBlk, true);

            blk = state.getBlockchain().getIndexedBlock(minedBlk.getHash());
        }

        // Check that the fork switch was successful
        assertNotNull(blk);
        assertTrue(state.getBlockchain().getMainChain().contains(blk));

        // Check that the transaction has become orphan and is not present in neither UTXO set
        assertTrue(state.getTransactionPool().isOrphan(tx.getHash()));
        assertFalse(state.getTransactionPool().hasValidTransaction(tx.getHash()));
        assertFalse(state.getChainUTXODatabase().isUnspent(tx.getHash(), 0));
        assertFalse(state.getPoolUTXODatabase().isUnspent(tx.getHash(), 0));
    }
}
