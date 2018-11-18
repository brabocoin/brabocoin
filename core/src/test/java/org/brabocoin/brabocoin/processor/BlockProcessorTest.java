package org.brabocoin.brabocoin.processor;

import org.brabocoin.brabocoin.Constants;
import org.brabocoin.brabocoin.chain.Blockchain;
import org.brabocoin.brabocoin.crypto.EllipticCurve;
import org.brabocoin.brabocoin.crypto.Signer;
import org.brabocoin.brabocoin.dal.*;
import org.brabocoin.brabocoin.exceptions.DatabaseException;
import org.brabocoin.brabocoin.model.*;
import org.brabocoin.brabocoin.node.config.BraboConfig;
import org.brabocoin.brabocoin.node.config.BraboConfigProvider;
import org.brabocoin.brabocoin.testutil.MockBraboConfig;
import org.brabocoin.brabocoin.testutil.Simulation;
import org.brabocoin.brabocoin.validation.Consensus;
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
    private CompositeReadonlyUTXOSet compositeUtxo;
    private Signer signer;

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
        consensus = new Consensus();
        utxoFromChain = new ChainUTXODatabase(new HashMapDB(), consensus);
        utxoFromPool = new UTXODatabase(new HashMapDB());
        utxoProcessor = new UTXOProcessor(utxoFromChain);
        transactionPool = new TransactionPool(config, new Random());
        blockchain = new Blockchain(new BlockDatabase(new HashMapDB(), config), consensus);
        compositeUtxo = new CompositeReadonlyUTXOSet(utxoFromChain, utxoFromPool);
        signer = new Signer(EllipticCurve.secp256k1());
        transactionValidator = new TransactionValidator(
                null, null, null, null, null,null
        );
        transactionProcessor = new TransactionProcessor(transactionValidator, transactionPool, utxoFromChain, utxoFromPool);
        blockValidator = new BlockValidator(
                null, null, null, null, null,null
        );
        blockProcessor = new BlockProcessor(blockchain, utxoProcessor, transactionProcessor, consensus, blockValidator);
    }

    @Disabled
    @Test
    void invalidBlock() throws DatabaseException {
        blockValidator = new BlockValidator(
                null, null, null, null, null,null
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

        blockProcessor = new BlockProcessor(blockchain, utxoProcessor, transactionProcessor, consensus, blockValidator);

        Block block = Simulation.randomBlockChainGenerator(1).get(0);

        ProcessedBlockStatus status = blockProcessor.processNewBlock(block);
        assertEquals(ProcessedBlockStatus.INVALID, status);
    }

    @Test
    @Disabled("Fails because transaction rules are not yet implemented.")
    void addAsOrphanParentUnknown() throws DatabaseException {
        Block block = Simulation.randomBlockChainGenerator(1).get(0);
        ProcessedBlockStatus status = blockProcessor.processNewBlock(block);

        assertEquals(ProcessedBlockStatus.ORPHAN, status);
        assertTrue(blockchain.isOrphan(block.getHash()));
    }

    @Test
    @Disabled("Fails because transaction rules are not yet implemented.")
    void addAsOrphanParentOrphan() throws DatabaseException {
        List<Block> blocks = Simulation.randomBlockChainGenerator(2);
        Block parent = blocks.get(0);
        Block child = blocks.get(1);

        blockchain.storeBlock(parent);
        blockchain.addOrphan(parent);

        ProcessedBlockStatus status = blockProcessor.processNewBlock(child);
        assertEquals(ProcessedBlockStatus.ORPHAN, status);
        assertTrue(blockchain.isOrphan(child.getHash()));
    }

    @Test
    void addCoinbaseToGenesis() throws DatabaseException {
        Output output = Simulation.randomOutput();
        Transaction transaction = new Transaction(
            Collections.emptyList(),
            Collections.singletonList(output)
        );
        Block block = new Block(
            consensus.getGenesisBlock().getHash(),
            Simulation.randomHash(),
            Simulation.randomHash(),
            Simulation.randomBigInteger(), 1,
            Collections.singletonList(transaction)
        );
        Hash hash = block.getHash();
        Hash tHash = transaction.getHash();

        // Add to transaction pool manually
        transactionPool.addIndependentTransaction(transaction);
        utxoFromPool.setOutputsUnspent(transaction, Constants.TRANSACTION_POOL_HEIGHT);

        ProcessedBlockStatus status = blockProcessor.processNewBlock(block);

        // Check chain
        assertEquals(ProcessedBlockStatus.VALID, status);
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
            consensus.getGenesisBlock().getHash(),
            Simulation.randomHash(),
            Simulation.randomHash(),
            Simulation.randomBigInteger(), 1,
            Collections.singletonList(transactionA)
        );
        Hash hashA = blockA.getHash();
        Hash tHashA = transactionA.getHash();

        Transaction transactionB = new Transaction(
            Collections.singletonList(new Input(Simulation.randomSignature(), tHashA, 0)),
            Collections.singletonList(Simulation.randomOutput())
        );
        Block blockB = new Block(
            hashA,
            Simulation.randomHash(),
            Simulation.randomHash(),
            Simulation.randomBigInteger(), 2,
            Collections.singletonList(transactionB)
        );
        Hash tHashB = transactionB.getHash();

        blockProcessor.processNewBlock(blockA);

        ProcessedBlockStatus status = blockProcessor.processNewBlock(blockB);

        // Check chain
        assertEquals(ProcessedBlockStatus.VALID, status);

        // Check UTXO
        assertTrue(utxoFromChain.isUnspent(tHashB, 0));
        assertFalse(utxoFromChain.isUnspent(tHashA, 0));
    }

    @Test
    void addAsFork() throws DatabaseException {
        // Main chain: genesis - A - B
        // Fork:              \_ C
        Block blockA = new Block(
            consensus.getGenesisBlock().getHash(),
            Simulation.randomHash(),
            Simulation.randomHash(),
            Simulation.randomBigInteger(), 1,
            Collections.singletonList(new Transaction(
                Collections.emptyList(),
                Collections.singletonList(Simulation.randomOutput())
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
                Collections.singletonList(Simulation.randomOutput())
            ))
        );
        Hash hashB = blockB.getHash();

        Transaction transaction = new Transaction(
            Collections.emptyList(),
            Collections.singletonList(Simulation.randomOutput())
        );
        Hash tHash = transaction.getHash();

        Block blockC = new Block(
            consensus.getGenesisBlock().getHash(),
            Simulation.randomHash(),
            Simulation.randomHash(),
            Simulation.randomBigInteger(), 1,
            Collections.singletonList(transaction)
        );
        Hash hashC = blockC.getHash();

        // Add to transaction pool manually
        transactionPool.addIndependentTransaction(transaction);
        utxoFromPool.setOutputsUnspent(transaction, Constants.TRANSACTION_POOL_HEIGHT);

        blockProcessor.processNewBlock(blockA);
        blockProcessor.processNewBlock(blockB);

        ProcessedBlockStatus status = blockProcessor.processNewBlock(blockC);

        // Check chain
        assertEquals(ProcessedBlockStatus.VALID, status);
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
    @Disabled("Fails because transaction rules are not yet implemented.")
    void addAsForkWithOrphansSwitch() throws DatabaseException {
        // Main chain: genesis - A - B - C
        // Fork:              \_ D - E - F - G

        Block blockA = new Block(
            consensus.getGenesisBlock().getHash(),
            Simulation.randomHash(),
            Simulation.randomHash(),
            Simulation.randomBigInteger(), 1,
            Collections.singletonList(new Transaction(
                Collections.emptyList(),
                Collections.singletonList(Simulation.randomOutput())
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
                Collections.singletonList(Simulation.randomOutput())
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
                Collections.singletonList(Simulation.randomOutput())
            ))
        );
        Hash hashC = blockC.getHash();

        Block blockD = new Block(
            consensus.getGenesisBlock().getHash(),
            Simulation.randomHash(),
            Simulation.randomHash(),
            Simulation.randomBigInteger(), 1,
            Collections.singletonList(new Transaction(
                Collections.emptyList(),
                Collections.singletonList(Simulation.randomOutput())
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
                Collections.singletonList(Simulation.randomOutput())
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
                Collections.singletonList(Simulation.randomOutput())
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
                Collections.singletonList(Simulation.randomOutput())
            ))
        );
        Hash hashG = blockG.getHash();

        blockProcessor.processNewBlock(blockA);
        blockProcessor.processNewBlock(blockB);
        blockProcessor.processNewBlock(blockC);
        blockProcessor.processNewBlock(blockD);
        blockProcessor.processNewBlock(blockF);
        blockProcessor.processNewBlock(blockG);

        ProcessedBlockStatus status = blockProcessor.processNewBlock(blockE);

        // Check chain
        assertEquals(ProcessedBlockStatus.VALID, status);
        assertEquals(hashG, blockchain.getMainChain().getTopBlock().getHash());
        assertEquals(4, blockchain.getMainChain().getHeight());

        assertTrue(blockchain.getMainChain().contains(blockchain.getIndexedBlock(hashD)));
        assertTrue(blockchain.getMainChain().contains(blockchain.getIndexedBlock(hashE)));
        assertTrue(blockchain.getMainChain().contains(blockchain.getIndexedBlock(hashF)));

        assertFalse(blockchain.getMainChain().contains(blockchain.getIndexedBlock(hashA)));
        assertFalse(blockchain.getMainChain().contains(blockchain.getIndexedBlock(hashB)));
        assertFalse(blockchain.getMainChain().contains(blockchain.getIndexedBlock(hashC)));

        // Check UTXO
        assertFalse(utxoFromChain.isUnspent(blockA.getTransactions().get(0).getHash(), 0));
        assertFalse(utxoFromChain.isUnspent(blockB.getTransactions().get(0).getHash(), 0));
        assertFalse(utxoFromChain.isUnspent(blockC.getTransactions().get(0).getHash(), 0));

        assertTrue(utxoFromChain.isUnspent(blockD.getTransactions().get(0).getHash(), 0));
        assertTrue(utxoFromChain.isUnspent(blockE.getTransactions().get(0).getHash(), 0));
        assertTrue(utxoFromChain.isUnspent(blockF.getTransactions().get(0).getHash(), 0));
        assertTrue(utxoFromChain.isUnspent(blockG.getTransactions().get(0).getHash(), 0));

        // Check transaction pool
        assertTrue(transactionPool.hasValidTransaction(blockA.getTransactions().get(0).getHash()));
        assertTrue(transactionPool.hasValidTransaction(blockB.getTransactions().get(0).getHash()));
        assertTrue(transactionPool.hasValidTransaction(blockC.getTransactions().get(0).getHash()));
    }

}
