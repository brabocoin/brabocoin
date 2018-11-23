package org.brabocoin.brabocoin.chain;

import org.brabocoin.brabocoin.dal.BlockDatabase;
import org.brabocoin.brabocoin.model.dal.BlockInfo;
import org.brabocoin.brabocoin.model.dal.BlockUndo;
import org.brabocoin.brabocoin.dal.HashMapDB;
import org.brabocoin.brabocoin.exceptions.DatabaseException;
import org.brabocoin.brabocoin.model.Block;
import org.brabocoin.brabocoin.model.Hash;
import org.brabocoin.brabocoin.node.config.BraboConfig;
import org.brabocoin.brabocoin.node.config.BraboConfigProvider;
import org.brabocoin.brabocoin.testutil.MockBraboConfig;
import org.brabocoin.brabocoin.testutil.Simulation;
import org.brabocoin.brabocoin.validation.Consensus;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test {@link Blockchain}.
 */
class BlockchainTest {

    private static final String BLOCK_FILE_LOCATION = "testenv/blocks";
    private static final @NotNull File blocksDirectory = new File(BLOCK_FILE_LOCATION);
    private static BraboConfig config;

    private static Block TEST_BLOCK;

    private BlockDatabase database;
    private Consensus consensus;
    private Blockchain blockchain;

    @BeforeAll
    static void setUpAll() throws DatabaseException {
        TEST_BLOCK = Simulation.randomBlockChainGenerator(1).get(0);

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
        consensus = new Consensus();
        database = new BlockDatabase(new HashMapDB(), new File(config.blockStoreDirectory()), config.maxBlockFileSize());
        blockchain = new Blockchain(database, consensus);
    }

    @AfterEach
    void tearDown() {
        // Remove block files
        for (File f : blocksDirectory.listFiles()) {
            f.delete();
        }
    }

    @Test
    void genesisBlock() throws DatabaseException {
        Hash hash = consensus.getGenesisBlock().getHash();
        assertTrue(blockchain.isBlockStored(hash));

        IndexedBlock topBlock = blockchain.getMainChain().getTopBlock();
        assertNotNull(topBlock);
        assertEquals(hash, topBlock.getHash());
    }

    @Test
    void getBlockByIndexedBlock() throws DatabaseException {
        Hash hash = consensus.getGenesisBlock().getHash();
        IndexedBlock block = blockchain.getIndexedBlock(hash);
        Block fromChain = blockchain.getBlock(block);

        assertNotNull(fromChain);
        assertEquals(hash, fromChain.getHash());
    }

    @Test
    void getBlockByHash() throws DatabaseException {
        Hash hash = consensus.getGenesisBlock().getHash();
        Block fromChain = blockchain.getBlock(hash);

        assertNotNull(fromChain);
        assertEquals(hash, fromChain.getHash());
    }

    @Test
    void isBlockStored() throws DatabaseException {
        assertTrue(blockchain.isBlockStored(consensus.getGenesisBlock().getHash()));
        assertFalse(blockchain.isBlockStored(TEST_BLOCK.getHash()));
    }

    @Test
    void storeBlock() throws DatabaseException {
        BlockInfo info = blockchain.storeBlock(TEST_BLOCK);
        assertNotNull(info);
    }

    @Test
    void getIndexedBlock() throws DatabaseException {
        Hash hash = consensus.getGenesisBlock().getHash();
        IndexedBlock block = blockchain.getIndexedBlock(hash);
        assertNotNull(block);
        assertEquals(hash, block.getHash());
    }

    @Test
    void getIndexedBlockNotPresent() throws DatabaseException {
        Hash hash = new Hash(TEST_BLOCK.getHash().getValue().substring(1));
        IndexedBlock block = blockchain.getIndexedBlock(hash);
        assertNull(block);
    }

    @Test
    void isOrphanNonExistent() {
        IndexedBlock block = Simulation.randomIndexedBlockChainGenerator(1).get(0);
        assertFalse(blockchain.isOrphan(block.getHash()));
    }

    @Test
    void addOrphan() {
        Block block = Simulation.randomBlockChainGenerator(1).get(0);
        blockchain.addOrphan(block);
        assertTrue(blockchain.isOrphan(block.getHash()));
    }

    @Test
    void findBlockUndoNonExistent() throws DatabaseException {
        Hash hash = new Hash(TEST_BLOCK.getHash().getValue().substring(1));
        assertNull(blockchain.findBlockUndo(hash));
    }

    @Test
    void storeFindBlockUndo() throws DatabaseException {
        IndexedBlock block = Simulation.randomIndexedBlockChainGenerator(1).get(0);
        blockchain.storeBlockUndo(block, new BlockUndo(new ArrayList<>()));

        assertNotNull(blockchain.findBlockUndo(block.getHash()));
    }

    @Test
    void removeOrphansOfParentEmpty() {
        List<Block> blocks = Simulation.randomBlockChainGenerator(2);
        Hash parent = blocks.get(0).getPreviousBlockHash();
        assertTrue(blockchain.removeOrphansOfParent(parent).isEmpty());

        blockchain.addOrphan(blocks.get(1));
        assertTrue(blockchain.removeOrphansOfParent(parent).isEmpty());
    }

    @Test
    void removeSingleOrphanOfParent() {
        List<Block> blocks = Simulation.randomBlockChainGenerator(2);
        Block orphan =  blocks.get(0);

        blockchain.addOrphan(orphan);
        blockchain.addOrphan(blocks.get(1));

        Set<Block> orphans = blockchain.removeOrphansOfParent(orphan.getPreviousBlockHash());

        assertEquals(1, orphans.size());
        assertEquals(orphan.getHash(), orphans.stream().findFirst().get().getHash());
    }

    @Test
    void removeMultipleOrphansOfParent() {
        Block block1 = Simulation.randomBlockChainGenerator(1).get(0);
        Hash parent = block1.getPreviousBlockHash();
        Block block2 = new Block(
                parent,
                block1.getMerkleRoot(),
                block1.getTargetValue(),
                block1.getNonce(),
                0,
                block1.getTransactions(),
                0);

        blockchain.addOrphan(block1);
        blockchain.addOrphan(block2);

        Set<Block> orphans = blockchain.removeOrphansOfParent(parent);
        assertEquals(2, orphans.size());
    }

    @Test
    void pushPopTopBlock() {
        IndexedBlock block = Simulation.randomIndexedBlockChainGenerator(1).get(0);
        blockchain.pushTopBlock(block);

        IndexedBlock fromChain = blockchain.popTopBlock();
        assertNotNull(fromChain);
        assertEquals(block.getHash(), fromChain.getHash());
    }

    @Test
    void getMainChain() {
        assertNotNull(blockchain.getMainChain());
    }
}
