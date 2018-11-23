package org.brabocoin.brabocoin.dal;

import org.brabocoin.brabocoin.chain.IndexedBlock;
import org.brabocoin.brabocoin.exceptions.DatabaseException;
import org.brabocoin.brabocoin.model.Block;
import org.brabocoin.brabocoin.model.Hash;
import org.brabocoin.brabocoin.model.dal.BlockFileInfo;
import org.brabocoin.brabocoin.model.dal.BlockInfo;
import org.brabocoin.brabocoin.model.dal.BlockUndo;
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
        database = new BlockDatabase(storage, new File(config.blockStoreDirectory()), config.maxBlockFileSize());
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
        Hash hash = block.getHash();

        database.storeBlock(block);
        Block retrievedBlock = database.findBlock(hash);

        assertBlock(hash, retrievedBlock);
    }

    @Test
    void storeAndFindMultipleBlocks() throws DatabaseException {
        List<Block> blocks = Simulation.randomBlockChainGenerator(3);
        for (Block block : blocks) {
            database.storeBlock(block);
        }

        for (Block block : blocks) {
            Hash hash = block.getHash();
            Block retrievedBlock = database.findBlock(hash);
            assertBlock(hash, retrievedBlock);
        }
    }

    @Test
    void storeBlockTwice() throws DatabaseException {
        Block block = Simulation.randomBlockChainGenerator(1).get(0);
        Hash hash = block.getHash();

        database.storeBlock(block);
        BlockInfo info1 = database.findBlockInfo(hash);
        assertNotNull(info1);

        database.storeBlock(block);
        BlockInfo info2 = database.findBlockInfo(hash);
        assertNotNull(info2);

        assertEquals(info1.getFileNumber(), info2.getFileNumber());
        assertEquals(info1.getOffsetInFile(), info2.getOffsetInFile());
        assertEquals(info1.getSizeInFile(), info2.getSizeInFile());
        assertEquals(info1.isValid(), info2.isValid());
    }

    @Test
    void storeBlockTwiceUpdateValidated() throws DatabaseException {
        Block block = Simulation.randomBlockChainGenerator(1).get(0);
        Hash hash = block.getHash();

        database.storeBlock(block);
        BlockInfo info1 = database.findBlockInfo(hash);
        assertNotNull(info1);

        database.storeBlock(block);
        BlockInfo info2 = database.findBlockInfo(hash);
        assertNotNull(info2);

        assertEquals(info1.getFileNumber(), info2.getFileNumber());
        assertEquals(info1.getOffsetInFile(), info2.getOffsetInFile());
        assertEquals(info1.getSizeInFile(), info2.getSizeInFile());
        assertTrue(info2.isValid());
    }

    @Test
    void setValidationStatus() throws DatabaseException {
        Block block = Simulation.randomBlockChainGenerator(1).get(0);
        Hash hash = block.getHash();

        database.storeBlock(block);

        BlockInfo info = database.findBlockInfo(hash);
        assertNotNull(info);
        assertTrue(info.isValid());

        BlockInfo info2 = database.setBlockInvalid(hash);
        assertFalse(info2.isValid());
    }

    @Test
    void setBlockInvalidUnknownBlock() {
        Hash hash = Simulation.randomHash();

        assertThrows(DatabaseException.class, () -> {
            database.setBlockInvalid(hash);
        });
    }

    private void assertBlock(Hash expectedHash, Block actualBlock) {
        assertNotNull(actualBlock);
        assertEquals(expectedHash, actualBlock.getHash());
    }

    @Test
    void findNonExistingBlock() throws DatabaseException {
        Block block = Simulation.randomBlockChainGenerator(1).get(0);
        database.storeBlock(block);

        Hash hash = new Hash(block.getHash().getValue().substring(1));
        Block nonExistent = database.findBlock(hash);

        assertNull(nonExistent);
    }

    @Test
    void storeAndFindBlockInfo() throws DatabaseException {
        Block block = Simulation.randomBlockChainGenerator(1).get(0);
        Hash hash = block.getHash();

        database.storeBlock(block);

        BlockInfo info = database.findBlockInfo(hash);
        assertNotNull(info);
        assertEquals(block.getPreviousBlockHash(), info.getPreviousBlockHash());
        assertEquals(block.getMerkleRoot(), info.getMerkleRoot());
        assertEquals(block.getTargetValue(), info.getTargetValue());
        assertEquals(block.getNonce(), info.getNonce());
        assertEquals(block.getTransactions().size(), info.getTransactionCount());
        assertTrue(info.isValid());
    }

    @Test
    void findNonExistingBlockInfo() throws DatabaseException {
        Block block = Simulation.randomBlockChainGenerator(1).get(0);
        database.storeBlock(block);

        Hash hash = new Hash(block.getHash().getValue().substring(1));
        BlockInfo nonExistent = database.findBlockInfo(hash);

        assertNull(nonExistent);
    }

    @Test
    void findBlockFileInfo() throws DatabaseException {
        Block block = Simulation.randomBlockChainGenerator(1).get(0);
        database.storeBlock(block);

        BlockFileInfo fileInfo = database.findBlockFileInfo(0);

        assertNotNull(fileInfo);
        assertEquals(1, fileInfo.getNumberOfBlocks());
        assertTrue(fileInfo.getSize() > 0);
        assertEquals(0, fileInfo.getLowestBlockHeight());
        assertEquals(0, fileInfo.getHighestBlockHeight());
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
            database.storeBlock(block);
        }

        BlockFileInfo fileInfo = database.findBlockFileInfo(0);

        assertNotNull(fileInfo);
        assertEquals(3, fileInfo.getNumberOfBlocks());
        assertTrue(fileInfo.getSize() > 0);
        assertEquals(0, fileInfo.getLowestBlockHeight());
        assertEquals(2, fileInfo.getHighestBlockHeight());
    }

    @Test
    void createNewFileWhenSizeExceeds() throws DatabaseException {
        BraboConfig smallConfig = new MockBraboConfig(config) {
            @Override
            public int maxBlockFileSize() {
                return 1;
            }
        };

        database = new BlockDatabase(storage, new File(smallConfig.blockStoreDirectory()), smallConfig.maxBlockFileSize());

        Block block = Simulation.randomBlockChainGenerator(1).get(0);
        Hash hash = block.getHash();

        database.storeBlock(block);

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
        Hash hash = block.getHash();

        BlockInfo info = database.storeBlock(block);
        assertNotNull(info);
        assertEquals(-1, info.getOffsetInUndoFile());
        assertEquals(-1, info.getSizeInUndoFile());

        IndexedBlock indexedBlock = new IndexedBlock(hash, info);
        BlockUndo undo = new BlockUndo(new ArrayList<>());

        database.storeBlockUndo(indexedBlock, undo);

        BlockInfo newInfo = database.findBlockInfo(hash);

        assertNotNull(newInfo);
        assertTrue(newInfo.getSizeInUndoFile() >= 0);

        BlockUndo retrievedUndo = database.findBlockUndo(hash);
        assertNotNull(retrievedUndo);
    }

    @Test
    void storeBlockUndoTwice() throws DatabaseException {
        Block block = Simulation.randomBlockChainGenerator(1).get(0);
        Hash hash = block.getHash();

        BlockInfo oldInfo = database.storeBlock(block);

        IndexedBlock indexedBlock = new IndexedBlock(hash, oldInfo);
        BlockUndo undo = new BlockUndo(new ArrayList<>());

        BlockInfo info = database.storeBlockUndo(indexedBlock, undo);
        BlockInfo newInfo = database.storeBlockUndo(indexedBlock, undo);

        assertEquals(info.getSizeInUndoFile(), newInfo.getSizeInUndoFile());
        assertEquals(info.getOffsetInUndoFile(), newInfo.getOffsetInUndoFile());
    }

    @Test
    void hasExistingBlock() throws DatabaseException {
        Block block = Simulation.randomBlockChainGenerator(1).get(0);
        Hash hash = block.getHash();

        database.storeBlock(block);

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

        BlockDatabase newDatabase = new BlockDatabase(storage, new File(newConfig.blockStoreDirectory()), newConfig.maxBlockFileSize());

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

        assertThrows(DatabaseException.class, () -> new BlockDatabase(storage, new File(newConfig.blockStoreDirectory()), newConfig.maxBlockFileSize()));
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

        assertThrows(DatabaseException.class, () -> new BlockDatabase(storage, new File(newConfig.blockStoreDirectory()), newConfig.maxBlockFileSize()));
    }

    @Test
    void findNonExistingUndo() throws DatabaseException {
        Hash hash = Simulation.randomHash();

        BlockUndo undo = database.findBlockUndo(hash);
        assertNull(undo);
    }
}
