package org.brabocoin.brabocoin.processor;

import org.brabocoin.brabocoin.chain.Blockchain;
import org.brabocoin.brabocoin.chain.IndexedBlock;
import org.brabocoin.brabocoin.dal.BlockDatabase;
import org.brabocoin.brabocoin.dal.HashMapDB;
import org.brabocoin.brabocoin.dal.ChainUTXODatabase;
import org.brabocoin.brabocoin.exceptions.DatabaseException;
import org.brabocoin.brabocoin.model.Block;
import org.brabocoin.brabocoin.model.Hash;
import org.brabocoin.brabocoin.node.config.BraboConfig;
import org.brabocoin.brabocoin.node.config.BraboConfigProvider;
import org.brabocoin.brabocoin.testutil.MockBraboConfig;
import org.brabocoin.brabocoin.testutil.Simulation;
import org.brabocoin.brabocoin.validation.BlockValidator;
import org.brabocoin.brabocoin.validation.Consensus;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
    private BlockValidator validator;
    private Consensus consensus;
    private BlockProcessor blockProcessor;

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
        validator = new BlockValidator();
        consensus = new Consensus();
        utxoProcessor = new UTXOProcessor(new ChainUTXODatabase(new HashMapDB(), consensus));
        blockchain = new Blockchain(new BlockDatabase(new HashMapDB(), config), consensus);
        blockProcessor = new BlockProcessor(blockchain, utxoProcessor, consensus, validator);
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
        validator = new BlockValidator() {
            @Override
            public boolean checkBlockValid(@NotNull Block block) {
                return false;
            }
        };

        blockProcessor = new BlockProcessor(blockchain, utxoProcessor, consensus, validator);

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
    @Disabled("Need more consensus.")
    void addCoinbaseToGenesis() throws DatabaseException {
        // TODO: coinbase block
        Hash genesis = consensus.getGenesisBlock().computeHash();
        Block block = Simulation.randomBlockChainGenerator(1, genesis, 1).get(0);
        Hash hash = block.computeHash();

        ProcessedBlockStatus status = blockProcessor.processNewBlock(block);

        assertEquals(ProcessedBlockStatus.ADDED_TO_BLOCKCHAIN, status);
        assertEquals(hash, blockchain.getMainChain().getTopBlock().getHash());
        assertEquals(1, blockchain.getMainChain().getHeight());
    }

    @Test
    @Disabled("Need more consensus.")
    void addCoinbaseToGenesisUTXOAdded() {
        // TODO: Add coinbase on top or genesis
    }

    @Test
    @Disabled("Need more consensus.")
    void addCoinbaseToGenesisMempoolUpdated() {
    }

    @Test
    @Disabled("Need more consensus.")
    void addAsFork() {
        // TODO: Regular block as fork
    }

    @Test
    @Disabled("Need more consensus.")
    void addAsForkUTXOUpdated() {
        // TODO: Regular block as fork UTXO added/removed
    }

    @Test
    @Disabled("Need more consensus.")
    void addAsForkMempoolUpdated() {
        // TODO: Regular block as fork mempool updated
    }

    @Test
    @Disabled("Need more consensus.")
    void addAsForkWithOrphansSwitch() {
        // TODO: reorganize with added orphans
    }

    @Test
    @Disabled("Need more consensus.")
    void addAsForkWithOrphansSwitchUTXOUpdated() {
        // TODO: reorganize with added orphans utxo added/removed
    }

    @Test
    @Disabled("Need more consensus.")
    void addAsForkWithOrphansSwitchMempoolUpdated() {
        // TODO: reorganize with added orphans mempool updated
    }
}
