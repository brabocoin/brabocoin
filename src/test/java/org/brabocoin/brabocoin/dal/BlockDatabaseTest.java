package org.brabocoin.brabocoin.dal;

import org.brabocoin.brabocoin.exceptions.DatabaseException;
import org.brabocoin.brabocoin.model.Block;
import org.brabocoin.brabocoin.model.Hash;
import org.brabocoin.brabocoin.node.config.BraboConfig;
import org.brabocoin.brabocoin.node.config.BraboConfigProvider;
import org.brabocoin.brabocoin.testutil.MockBraboConfig;
import org.brabocoin.brabocoin.testutil.Simulation;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test the block database.
 */
class BlockDatabaseTest {

    private static final String BLOCK_FILE_LOCATION = "testenv/blocks";
    private static final @NotNull File blocksDirectory = new File(BLOCK_FILE_LOCATION);
    private static BraboConfig config;

    private BlockDatabase database;
    private KeyValueStore storage;

    @BeforeAll
    static void setUpEnvironment() {
        BraboConfig defaultConfig = BraboConfigProvider.getConfig().bind("brabo", BraboConfig.class);
        config = new MockBraboConfig(defaultConfig) {
            @Override
            public String blockStoreDirectory() {
                return BLOCK_FILE_LOCATION;
            }
        };

        blocksDirectory.mkdirs();
    }

    @BeforeEach
    void setUp() throws DatabaseException {
        storage = new HashMapDB();
        database = new BlockDatabase(storage, config);
    }

    @AfterEach
    void tearDown() {
        // Remove block files
        for (File f : blocksDirectory.listFiles()) {
            f.delete();
        }
    }

    @Test
    void storeAndFindBlock() throws DatabaseException {
        Block block = Simulation.randomBlockChainGenerator(1).get(0);
        Hash hash = block.computeHash();

        database.storeBlock(block, false);
        Block retrievedBlock = database.findBlock(hash);

        assertBlock(hash, retrievedBlock);
    }

    @Test
    void storeAndFindMultipleBlocks() throws DatabaseException {
        List<Block> blocks = Simulation.randomBlockChainGenerator(3);
        for (Block block : blocks) {
            database.storeBlock(block, false);
        }

        for (Block block : blocks) {
            Hash hash = block.computeHash();
            Block retrievedBlock = database.findBlock(hash);
            assertBlock(hash, retrievedBlock);
        }
    }

    private void assertBlock(Hash expectedHash, Block actualBlock) {
        assertNotNull(actualBlock);
        assertEquals(expectedHash, actualBlock.computeHash());
    }

    @Test
    void findNonExistingBlock() throws DatabaseException {
        Block block = Simulation.randomBlockChainGenerator(1).get(0);
        database.storeBlock(block, false);

        Hash hash = new Hash(block.computeHash().getValue().substring(1));
        Block nonExistent = database.findBlock(hash);

        assertNull(nonExistent);
    }

    @Test
    void storeAndFindBlockInfo() throws DatabaseException {
        Block block = Simulation.randomBlockChainGenerator(1).get(0);
        Hash hash = block.computeHash();

        database.storeBlock(block, true);

        BlockInfo info = database.findBlockInfo(hash);
        assertNotNull(info);
        assertEquals(block.getPreviousBlockHash(), info.getPreviousBlockHash());
        assertEquals(block.getMerkleRoot(), info.getMerkleRoot());
        assertEquals(block.getTargetValue(), info.getTargetValue());
        assertEquals(block.getNonce(), info.getNonce());
        assertEquals(block.getTimestamp(), info.getTimestamp());
        assertEquals(block.getBlockHeight(), info.getBlockHeight());
        assertEquals(block.getTransactions().size(), info.getTransactionCount());
        assertTrue(info.isValidated());
    }

    @Test
    void findNonExistingBlockInfo() throws DatabaseException {
        Block block = Simulation.randomBlockChainGenerator(1).get(0);
        database.storeBlock(block, false);

        Hash hash = new Hash(block.computeHash().getValue().substring(1));
        BlockInfo nonExistent = database.findBlockInfo(hash);

        assertNull(nonExistent);
    }

    @Test
    void findBlockFileInfo() throws DatabaseException {
        Block block = Simulation.randomBlockChainGenerator(1).get(0);
        database.storeBlock(block, false);

        BlockFileInfo fileInfo = database.findBlockFileInfo(0);

        assertNotNull(fileInfo);
        assertEquals(1, fileInfo.getNumberOfBlocks());
        assertTrue(fileInfo.getSize() > 0);
        assertEquals(0, fileInfo.getLowestBlockHeight());
        assertEquals(0, fileInfo.getHighestBlockHeight());
        assertEquals(block.getTimestamp(), fileInfo.getLowestBlockTimestamp());
        assertEquals(block.getTimestamp(), fileInfo.getHighestBlockTimestamp());
    }

    @Test
    void findNonExistingBlockFileInfo() throws DatabaseException {
        assertNull(database.findBlockFileInfo(1));
        assertNull(database.findBlockFileInfo(-1));
    }

    @Test
    void findBlockFileInfoWithMultipleBlocks() throws DatabaseException {
        List<Block> blocks = Simulation.randomBlockChainGenerator(3);
        for (Block block : blocks) {
            database.storeBlock(block, false);
        }

        BlockFileInfo fileInfo = database.findBlockFileInfo(0);
        long timestamp = blocks.get(0).getTimestamp();

        assertNotNull(fileInfo);
        assertEquals(3, fileInfo.getNumberOfBlocks());
        assertTrue(fileInfo.getSize() > 0);
        assertEquals(0, fileInfo.getLowestBlockHeight());
        assertEquals(2, fileInfo.getHighestBlockHeight());
        assertEquals(timestamp, fileInfo.getLowestBlockTimestamp());
        assertEquals(timestamp, fileInfo.getHighestBlockTimestamp());
    }

    @Test
    void createNewFileWhenSizeExceeds() throws DatabaseException {
        BraboConfig smallConfig = new MockBraboConfig(config) {
            @Override
            public long maxBlockFileSize() {
                return 1L;
            }
        };

        database = new BlockDatabase(storage, smallConfig);

        Block block = Simulation.randomBlockChainGenerator(1).get(0);
        Hash hash = block.computeHash();

        database.storeBlock(block, false);

        BlockInfo info = database.findBlockInfo(hash);
        assertNotNull(info);
        assertEquals(1, info.getFileNumber());

        BlockFileInfo fileInfo = database.findBlockFileInfo(1);
        assertNotNull(fileInfo);
        assertTrue(fileInfo.getSize() > 0);
    }
}
