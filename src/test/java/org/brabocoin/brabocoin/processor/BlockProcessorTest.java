package org.brabocoin.brabocoin.processor;

import org.brabocoin.brabocoin.Constants;
import org.brabocoin.brabocoin.chain.Blockchain;
import org.brabocoin.brabocoin.chain.IndexedBlock;
import org.brabocoin.brabocoin.dal.BlockDatabase;
import org.brabocoin.brabocoin.dal.ChainUTXODatabase;
import org.brabocoin.brabocoin.dal.HashMapDB;
import org.brabocoin.brabocoin.dal.TransactionPool;
import org.brabocoin.brabocoin.dal.UTXODatabase;
import org.brabocoin.brabocoin.exceptions.DatabaseException;
import org.brabocoin.brabocoin.model.Block;
import org.brabocoin.brabocoin.model.Hash;
import org.brabocoin.brabocoin.model.Input;
import org.brabocoin.brabocoin.model.Output;
import org.brabocoin.brabocoin.model.Transaction;
import org.brabocoin.brabocoin.node.config.BraboConfig;
import org.brabocoin.brabocoin.node.config.BraboConfigProvider;
import org.brabocoin.brabocoin.testutil.MockBraboConfig;
import org.brabocoin.brabocoin.testutil.Simulation;
import org.brabocoin.brabocoin.validation.block.BlockValidator;
import org.brabocoin.brabocoin.validation.Consensus;
import org.brabocoin.brabocoin.validation.transaction.TransactionValidator;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test {@link BlockProcessor}.
 */
class BlockProcessorTest {

    private static final String BLOCK_FILE_LOCATION = "testenv/blocks";
    private static final @NotNull File blocksDirectory = new File(BLOCK_FILE_LOCATION);
    private static BraboConfig config;

    private UTXOProcessor utxoProcessor;
    private Blockchain blockchain;
    private BlockValidator blockValidator;
    private TransactionValidator transactionValidator;
    private Consensus consensus;
    private BlockProcessor blockProcessor;
    private TransactionProcessor transactionProcessor;
    private TransactionPool transactionPool;
    private ChainUTXODatabase utxoFromChain;
    private UTXODatabase utxoFromPool;

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

    @BeforeEach
    void setUp() throws DatabaseException {
        blockValidator = new BlockValidator();
        transactionValidator = new TransactionValidator();
        consensus = new Consensus();
        utxoFromChain = new ChainUTXODatabase(new HashMapDB(), consensus);
        utxoFromPool = new UTXODatabase(new HashMapDB());
        utxoProcessor = new UTXOProcessor(utxoFromChain);
        transactionPool = new TransactionPool(config, new Random());
        transactionProcessor = new TransactionProcessor(transactionValidator, transactionPool, utxoFromChain, utxoFromPool);
        blockchain = new Blockchain(new BlockDatabase(new HashMapDB(), config), consensus);
        blockProcessor = new BlockProcessor(blockchain, utxoProcessor, transactionProcessor, consensus, blockValidator);
    }

    @AfterEach
    void tearDown() {
        // Remove block files
        for (File f : blocksDirectory.listFiles()) {
            f.delete();
        }
    }

    @Test
    void invalidBlock() throws DatabaseException {
        blockValidator = new BlockValidator() {
            @Override
            public boolean checkBlockValid(@NotNull Block block) {
                return false;
            }
        };

        blockProcessor = new BlockProcessor(blockchain, utxoProcessor, transactionProcessor, consensus, blockValidator);

        Block block = Simulation.randomBlockChainGenerator(1).get(0);

        ProcessedBlockStatus status = blockProcessor.processNewBlock(block);
        assertEquals(ProcessedBlockStatus.INVALID, status);
    }

    @Test
    void alreadyStored() throws DatabaseException {
        Block block = Simulation.randomBlockChainGenerator(1).get(0);
        blockchain.storeBlock(block, true);

        ProcessedBlockStatus status = blockProcessor.processNewBlock(block);
        assertEquals(ProcessedBlockStatus.ALREADY_STORED, status);
    }

    @Test
    void addAsOrphanParentUnknown() throws DatabaseException {
        Block block = Simulation.randomBlockChainGenerator(1).get(0);
        ProcessedBlockStatus status = blockProcessor.processNewBlock(block);

        assertEquals(ProcessedBlockStatus.ORPHAN, status);
        IndexedBlock indexedBlock = blockchain.getIndexedBlock(block.computeHash());
        assertTrue(blockchain.isOrphan(indexedBlock));
    }

    @Test
    void addAsOrphanParentOrphan() throws DatabaseException {
        List<Block> blocks = Simulation.randomBlockChainGenerator(2);
        Block parent = blocks.get(0);
        Block child = blocks.get(1);

        blockchain.storeBlock(parent, false);
        IndexedBlock indexedParent = blockchain.getIndexedBlock(parent.computeHash());
        blockchain.addOrphan(indexedParent);

        ProcessedBlockStatus status = blockProcessor.processNewBlock(child);
        assertEquals(ProcessedBlockStatus.ORPHAN, status);
        IndexedBlock indexedChild = blockchain.getIndexedBlock(child.computeHash());
        assertTrue(blockchain.isOrphan(indexedChild));
    }

    @Test
    void addCoinbaseToGenesis() throws DatabaseException {
        Output output = Simulation.randomOutput();
        Transaction transaction = new Transaction(
            Collections.emptyList(),
            Collections.singletonList(output)
        );
        Block block = new Block(
            consensus.getGenesisBlock().computeHash(),
            Simulation.randomHash(),
            Simulation.randomHash(),
            Simulation.randomByteString(), 1,
            Collections.singletonList(transaction)
        );
        Hash hash = block.computeHash();
        Hash tHash = transaction.computeHash();

        // Add to transaction pool manually
        transactionPool.addIndependentTransaction(transaction);
        utxoFromPool.setOutputsUnspent(transaction, Constants.TRANSACTION_POOL_HEIGHT);

        ProcessedBlockStatus status = blockProcessor.processNewBlock(block);

        // Check chain
        assertEquals(ProcessedBlockStatus.ADDED_TO_BLOCKCHAIN, status);
        assertEquals(hash, blockchain.getMainChain().getTopBlock().getHash());
        assertEquals(1, blockchain.getMainChain().getHeight());

        // Check UTXO
        assertTrue(utxoFromChain.isUnspent(tHash, 0));

        // Check transaction pool
        assertFalse(transactionPool.hasValidTransaction(tHash));
        assertFalse(utxoFromPool.isUnspent(tHash, 0));

        // Check undo data
        assertNotNull(blockchain.findBlockUndo(hash));
    }

    @Test
    void connectNonCoinbase() throws DatabaseException {
        Output output = Simulation.randomOutput();
        Transaction transactionA = new Transaction(
            Collections.emptyList(),
            Collections.singletonList(output)
        );
        Block blockA = new Block(
            consensus.getGenesisBlock().computeHash(),
            Simulation.randomHash(),
            Simulation.randomHash(),
            Simulation.randomByteString(), 1,
            Collections.singletonList(transactionA)
        );
        Hash hashA = blockA.computeHash();
        Hash tHashA = transactionA.computeHash();

        Transaction transactionB = new Transaction(
            Collections.singletonList(new Input(Simulation.randomSignature(), tHashA, 0)),
            Collections.singletonList(Simulation.randomOutput())
        );
        Block blockB = new Block(
            hashA,
            Simulation.randomHash(),
            Simulation.randomHash(),
            Simulation.randomByteString(), 2,
            Collections.singletonList(transactionB)
        );
        Hash tHashB = transactionB.computeHash();

        blockProcessor.processNewBlock(blockA);

        ProcessedBlockStatus status = blockProcessor.processNewBlock(blockB);

        // Check chain
        assertEquals(ProcessedBlockStatus.ADDED_TO_BLOCKCHAIN, status);

        // Check UTXO
        assertTrue(utxoFromChain.isUnspent(tHashB, 0));
        assertFalse(utxoFromChain.isUnspent(tHashA, 0));
    }

    @Test
    void addAsFork() throws DatabaseException {
        // Main chain: genesis - A - B
        // Fork:              \_ C
        Block blockA = new Block(
            consensus.getGenesisBlock().computeHash(),
            Simulation.randomHash(),
            Simulation.randomHash(),
            Simulation.randomByteString(), 1,
            Collections.singletonList(new Transaction(
                Collections.emptyList(),
                Collections.singletonList(Simulation.randomOutput())
            ))
        );
        Hash hashA = blockA.computeHash();

        Block blockB = new Block(
            hashA,
            Simulation.randomHash(),
            Simulation.randomHash(),
            Simulation.randomByteString(), 2,
            Collections.singletonList(new Transaction(
                Collections.emptyList(),
                Collections.singletonList(Simulation.randomOutput())
            ))
        );
        Hash hashB = blockB.computeHash();

        Transaction transaction = new Transaction(
            Collections.emptyList(),
            Collections.singletonList(Simulation.randomOutput())
        );
        Hash tHash = transaction.computeHash();

        Block blockC = new Block(
            consensus.getGenesisBlock().computeHash(),
            Simulation.randomHash(),
            Simulation.randomHash(),
            Simulation.randomByteString(), 1,
            Collections.singletonList(transaction)
        );
        Hash hashC = blockC.computeHash();

        // Add to transaction pool manually
        transactionPool.addIndependentTransaction(transaction);
        utxoFromPool.setOutputsUnspent(transaction, Constants.TRANSACTION_POOL_HEIGHT);

        blockProcessor.processNewBlock(blockA);
        blockProcessor.processNewBlock(blockB);

        ProcessedBlockStatus status = blockProcessor.processNewBlock(blockC);

        // Check chain
        assertEquals(ProcessedBlockStatus.ADDED_TO_BLOCKCHAIN, status);
        assertEquals(hashB, blockchain.getMainChain().getTopBlock().getHash());
        assertEquals(2, blockchain.getMainChain().getHeight());
        assertTrue(blockchain.isBlockStored(hashC));

        // Check UTXO
        assertFalse(utxoFromChain.isUnspent(tHash, 0));

        // Check transaction pool
        assertTrue(transactionPool.hasValidTransaction(tHash));
        assertTrue(utxoFromPool.isUnspent(tHash, 0));

        // Check undo data
        assertNull(blockchain.findBlockUndo(hashC));
    }

    @Test
    void addAsForkWithOrphansSwitch() throws DatabaseException {
        // Main chain: genesis - A - B - C
        // Fork:              \_ D - E - F - G

        Block blockA = new Block(
            consensus.getGenesisBlock().computeHash(),
            Simulation.randomHash(),
            Simulation.randomHash(),
            Simulation.randomByteString(), 1,
            Collections.singletonList(new Transaction(
                Collections.emptyList(),
                Collections.singletonList(Simulation.randomOutput())
            ))
        );
        Hash hashA = blockA.computeHash();

        Block blockB = new Block(
            hashA,
            Simulation.randomHash(),
            Simulation.randomHash(),
            Simulation.randomByteString(), 2,
            Collections.singletonList(new Transaction(
                Collections.emptyList(),
                Collections.singletonList(Simulation.randomOutput())
            ))
        );
        Hash hashB = blockB.computeHash();

        Block blockC = new Block(
            hashB,
            Simulation.randomHash(),
            Simulation.randomHash(),
            Simulation.randomByteString(), 3,
            Collections.singletonList(new Transaction(
                Collections.emptyList(),
                Collections.singletonList(Simulation.randomOutput())
            ))
        );
        Hash hashC = blockC.computeHash();

        Block blockD = new Block(
            consensus.getGenesisBlock().computeHash(),
            Simulation.randomHash(),
            Simulation.randomHash(),
            Simulation.randomByteString(), 1,
            Collections.singletonList(new Transaction(
                Collections.emptyList(),
                Collections.singletonList(Simulation.randomOutput())
            ))
        );
        Hash hashD = blockD.computeHash();

        Block blockE = new Block(
            hashD,
            Simulation.randomHash(),
            Simulation.randomHash(),
            Simulation.randomByteString(), 2,
            Collections.singletonList(new Transaction(
                Collections.emptyList(),
                Collections.singletonList(Simulation.randomOutput())
            ))
        );
        Hash hashE = blockE.computeHash();

        Block blockF = new Block(
            hashE,
            Simulation.randomHash(),
            Simulation.randomHash(),
            Simulation.randomByteString(), 3,
            Collections.singletonList(new Transaction(
                Collections.emptyList(),
                Collections.singletonList(Simulation.randomOutput())
            ))
        );
        Hash hashF = blockF.computeHash();


        Block blockG = new Block(
            hashF,
            Simulation.randomHash(),
            Simulation.randomHash(),
            Simulation.randomByteString(), 4,
            Collections.singletonList(new Transaction(
                Collections.emptyList(),
                Collections.singletonList(Simulation.randomOutput())
            ))
        );
        Hash hashG = blockG.computeHash();

        blockProcessor.processNewBlock(blockA);
        blockProcessor.processNewBlock(blockB);
        blockProcessor.processNewBlock(blockC);
        blockProcessor.processNewBlock(blockD);
        blockProcessor.processNewBlock(blockF);
        blockProcessor.processNewBlock(blockG);

        ProcessedBlockStatus status = blockProcessor.processNewBlock(blockE);

        // Check chain
        assertEquals(ProcessedBlockStatus.ADDED_TO_BLOCKCHAIN, status);
        assertEquals(hashG, blockchain.getMainChain().getTopBlock().getHash());
        assertEquals(4, blockchain.getMainChain().getHeight());

        assertTrue(blockchain.getMainChain().contains(blockchain.getIndexedBlock(hashD)));
        assertTrue(blockchain.getMainChain().contains(blockchain.getIndexedBlock(hashE)));
        assertTrue(blockchain.getMainChain().contains(blockchain.getIndexedBlock(hashF)));

        assertFalse(blockchain.getMainChain().contains(blockchain.getIndexedBlock(hashA)));
        assertFalse(blockchain.getMainChain().contains(blockchain.getIndexedBlock(hashB)));
        assertFalse(blockchain.getMainChain().contains(blockchain.getIndexedBlock(hashC)));

        // Check UTXO
        assertFalse(utxoFromChain.isUnspent(blockA.getTransactions().get(0).computeHash(), 0));
        assertFalse(utxoFromChain.isUnspent(blockB.getTransactions().get(0).computeHash(), 0));
        assertFalse(utxoFromChain.isUnspent(blockC.getTransactions().get(0).computeHash(), 0));

        assertTrue(utxoFromChain.isUnspent(blockD.getTransactions().get(0).computeHash(), 0));
        assertTrue(utxoFromChain.isUnspent(blockE.getTransactions().get(0).computeHash(), 0));
        assertTrue(utxoFromChain.isUnspent(blockF.getTransactions().get(0).computeHash(), 0));
        assertTrue(utxoFromChain.isUnspent(blockG.getTransactions().get(0).computeHash(), 0));

        // Check transaction pool
        assertTrue(transactionPool.hasValidTransaction(blockA.getTransactions().get(0).computeHash()));
        assertTrue(transactionPool.hasValidTransaction(blockB.getTransactions().get(0).computeHash()));
        assertTrue(transactionPool.hasValidTransaction(blockC.getTransactions().get(0).computeHash()));
    }

}
