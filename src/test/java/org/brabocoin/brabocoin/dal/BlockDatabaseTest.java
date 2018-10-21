package org.brabocoin.brabocoin.dal;

import org.brabocoin.brabocoin.chain.IndexedBlock;
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
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
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

    @Test
    void storeBlockTwice() throws DatabaseException {
        Block block = Simulation.randomBlockChainGenerator(1).get(0);
        Hash hash = block.computeHash();

        database.storeBlock(block, false);
        BlockInfo info1 = database.findBlockInfo(hash);
        assertNotNull(info1);

        database.storeBlock(block, false);
        BlockInfo info2 = database.findBlockInfo(hash);
        assertNotNull(info2);

        assertEquals(info1.getFileNumber(), info2.getFileNumber());
        assertEquals(info1.getOffsetInFile(), info2.getOffsetInFile());
        assertEquals(info1.getSizeInFile(), info2.getSizeInFile());
        assertEquals(info1.isValidated(), info2.isValidated());
    }

    @Test
    void storeBlockTwiceUpdateValidated() throws DatabaseException {
        Block block = Simulation.randomBlockChainGenerator(1).get(0);
        Hash hash = block.computeHash();

        database.storeBlock(block, false);
        BlockInfo info1 = database.findBlockInfo(hash);
        assertNotNull(info1);

        database.storeBlock(block, true);
        BlockInfo info2 = database.findBlockInfo(hash);
        assertNotNull(info2);

        assertEquals(info1.getFileNumber(), info2.getFileNumber());
        assertEquals(info1.getOffsetInFile(), info2.getOffsetInFile());
        assertEquals(info1.getSizeInFile(), info2.getSizeInFile());
        assertTrue(info2.isValidated());
    }

    @Test
    void setValidationStatus() throws DatabaseException {
        Block block = Simulation.randomBlockChainGenerator(1).get(0);
        Hash hash = block.computeHash();

        database.storeBlock(block, false);

        database.setBlockValidationStatus(hash, true);
        BlockInfo info = database.findBlockInfo(hash);
        assertNotNull(info);
        assertTrue(info.isValidated());

        database.setBlockValidationStatus(hash, false);
        BlockInfo info2 = database.findBlockInfo(hash);
        assertNotNull(info2);
        assertFalse(info2.isValidated());
    }

    @Test
    void setValidationStatusUnknownBlock() {
        Hash hash = Simulation.randomHash();

        assertThrows(DatabaseException.class, () -> {
            database.setBlockValidationStatus(hash, true);
        });
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

    @Test
    void storeAndFindBlockUndo() throws DatabaseException {
        Block block = Simulation.randomBlockChainGenerator(1).get(0);
        Hash hash = block.computeHash();

        BlockInfo info = database.storeBlock(block, false);
        assertNotNull(info);
        assertEquals(0, info.getOffsetInRevertFile());
        assertEquals(0, info.getSizeInRevertFile());

        IndexedBlock indexedBlock = new IndexedBlock(hash, info);
        BlockUndo undo = new BlockUndo(new ArrayList<>());

        database.storeBlockUndo(indexedBlock, undo);

        BlockInfo newInfo = database.findBlockInfo(hash);

        assertNotNull(newInfo);
        assertTrue(info.getSizeInFile() > 0);

        BlockUndo retrievedUndo = database.findBlockUndo(hash);
        assertNotNull(retrievedUndo);
    }

    @Test
    void hasExistingBlock() throws DatabaseException {
        Block block = Simulation.randomBlockChainGenerator(1).get(0);
        Hash hash = block.computeHash();

        database.storeBlock(block, false);

        assertTrue(database.hasBlock(hash));
    }

    @Test
    void notHasNonexistingBlock() throws DatabaseException {
        Hash hash = Simulation.randomHash();

        assertFalse(database.hasBlock(hash));
    }

    @Test
    void createNewDirectory() throws DatabaseException {
        final String newDir = BLOCK_FILE_LOCATION + "/stroomboot";

        BraboConfig newConfig = new MockBraboConfig(config) {
            @Override
            public String blockStoreDirectory() {
                return newDir;
            }
        };

        BlockDatabase newDatabase = new BlockDatabase(storage, newConfig);

        File dir = new File(newDir);
        assertTrue(dir.exists());

        // Cleanup
        dir.delete();
    }

    @Test
    void cannotCreateDirectory() {
        final String newDir = "/\0/invalid";

        BraboConfig newConfig = new MockBraboConfig(config) {
            @Override
            public String blockStoreDirectory() {
                return newDir;
            }
        };

        assertThrows(DatabaseException.class, () -> new BlockDatabase(storage, newConfig));
    }

    @Test
    void isNotDirectory() {
        final String newDir = "src/test/java/org/brabocoin/brabocoin/dal/BlockDatabaseTest.java";

        BraboConfig newConfig = new MockBraboConfig(config) {
            @Override
            public String blockStoreDirectory() {
                return newDir;
            }
        };

        assertThrows(DatabaseException.class, () -> new BlockDatabase(storage, newConfig));
    }

    @Test
    void findNonExistingUndo() throws DatabaseException {
        Hash hash = Simulation.randomHash();

        BlockUndo undo = database.findBlockUndo(hash);
        assertNull(undo);
    }
}
