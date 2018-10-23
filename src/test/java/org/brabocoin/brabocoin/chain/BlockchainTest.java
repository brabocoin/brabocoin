package org.brabocoin.brabocoin.chain;

import org.brabocoin.brabocoin.dal.BlockDatabase;
import org.brabocoin.brabocoin.dal.BlockInfo;
import org.brabocoin.brabocoin.dal.BlockUndo;
import org.brabocoin.brabocoin.dal.HashMapDB;
import org.brabocoin.brabocoin.exceptions.DatabaseException;
import org.brabocoin.brabocoin.model.Block;
import org.brabocoin.brabocoin.model.Hash;
import org.brabocoin.brabocoin.node.config.BraboConfig;
import org.brabocoin.brabocoin.node.config.BraboConfigProvider;
import org.brabocoin.brabocoin.testutil.Simulation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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

    private static Block TEST_BLOCK;

    private static BlockDatabase database;

    private static boolean blockUndoStored;
    private Blockchain blockchain;

    @BeforeAll
    static void setUpAll() throws DatabaseException {
        TEST_BLOCK = Simulation.randomBlockChainGenerator(1).get(0);

        BraboConfig config = BraboConfigProvider.getConfig().bind("brabo", BraboConfig.class);


        database = new BlockDatabase(new HashMapDB(), config) {
            @Override
            public @Nullable Block findBlock(@NotNull Hash hash) {
                return TEST_BLOCK;
            }

            @Override
            public boolean hasBlock(@NotNull Hash hash) throws DatabaseException {
                return TEST_BLOCK.computeHash().equals(hash);
            }

            @Override
            public @NotNull BlockInfo storeBlock(@NotNull Block block, boolean validated) throws DatabaseException {
                return this.findBlockInfo(block.computeHash());
            }

            @Override
            public BlockInfo findBlockInfo(@NotNull Hash hash) throws DatabaseException {
                if (!hash.equals(TEST_BLOCK.computeHash())) {
                    return null;
                }

                return new BlockInfo(
                    TEST_BLOCK.getPreviousBlockHash(),
                    TEST_BLOCK.getMerkleRoot(),
                    TEST_BLOCK.getTargetValue(),
                    TEST_BLOCK.getNonce(),
                    TEST_BLOCK.getTimestamp(),
                    TEST_BLOCK.getBlockHeight(),
                    TEST_BLOCK.getTransactions().size(),
                    true,
                    0,
                    0,
                    0,
                    0,
                    0
                );
            }

            @Override
            public @Nullable BlockUndo findBlockUndo(@NotNull Hash hash) throws DatabaseException {
                if (!hash.equals(TEST_BLOCK.computeHash())) {
                    return null;
                }

                return new BlockUndo(new ArrayList<>());
            }

            @Override
            public @NotNull BlockInfo storeBlockUndo(@NotNull IndexedBlock block,
                                                     @NotNull BlockUndo undo) throws DatabaseException {
                blockUndoStored = true;

                return this.findBlockInfo(TEST_BLOCK.computeHash());
            }
        };
    }

    @BeforeEach
    void setUp() {
        blockUndoStored = false;
        blockchain = new Blockchain(database);
    }

    @Test
    void getBlockByIndexedBlock() throws DatabaseException {
        IndexedBlock block = Simulation.randomIndexedBlockChainGenerator(1).get(0);
        Block fromChain = blockchain.getBlock(block);

        assertNotNull(fromChain);
        assertEquals(TEST_BLOCK.computeHash(), fromChain.computeHash());
    }

    @Test
    void getBlockByHash() throws DatabaseException {
        Hash hash = TEST_BLOCK.computeHash();
        Block fromChain = blockchain.getBlock(hash);

        assertNotNull(fromChain);
        assertEquals(hash, fromChain.computeHash());
    }

    @Test
    void isBlockStored() throws DatabaseException {
        assertTrue(blockchain.isBlockStored(TEST_BLOCK.computeHash()));
        Hash hash = new Hash(TEST_BLOCK.computeHash().getValue().substring(1));
        assertFalse(blockchain.isBlockStored(hash));
    }

    @Test
    void storeBlock() throws DatabaseException {
        BlockInfo info = blockchain.storeBlock(TEST_BLOCK, true);
        assertNotNull(info);
    }

    @Test
    void getIndexedBlock() throws DatabaseException {
        IndexedBlock block = blockchain.getIndexedBlock(TEST_BLOCK.computeHash());
        assertNotNull(block);
        assertEquals(block.getHash(), TEST_BLOCK.computeHash());
    }

    @Test
    void getIndexedBlockNotPresent() throws DatabaseException {
        Hash hash = new Hash(TEST_BLOCK.computeHash().getValue().substring(1));
        IndexedBlock block = blockchain.getIndexedBlock(hash);
        assertNull(block);
    }

    @Test
    void isOrphanNonExistent() {
        IndexedBlock block = Simulation.randomIndexedBlockChainGenerator(1).get(0);
        assertFalse(blockchain.isOrphan(block));
    }

    @Test
    void addOrphan() {
        IndexedBlock block = Simulation.randomIndexedBlockChainGenerator(1).get(0);
        blockchain.addOrphan(block);
        assertTrue(blockchain.isOrphan(block));
    }

    @Test
    void findBlockUndoNonExistent() throws DatabaseException {
        Hash hash = new Hash(TEST_BLOCK.computeHash().getValue().substring(1));
        assertNull(blockchain.findBlockUndo(hash));
    }

    @Test
    void findBlockUndo() throws DatabaseException {
        assertNotNull(blockchain.findBlockUndo(TEST_BLOCK.computeHash()));
    }

    @Test
    void storeBlockUndo() throws DatabaseException {
        IndexedBlock block = Simulation.randomIndexedBlockChainGenerator(1).get(0);
        blockchain.storeBlockUndo(block, new BlockUndo(new ArrayList<>()));

        assertTrue(blockUndoStored);
    }

    @Test
    void removeOrphansOfParentEmpty() {
        List<IndexedBlock> blocks = Simulation.randomIndexedBlockChainGenerator(2);
        Hash parent = blocks.get(0).getBlockInfo().getPreviousBlockHash();
        assertTrue(blockchain.removeOrphansOfParent(parent).isEmpty());

        blockchain.addOrphan(blocks.get(1));
        assertTrue(blockchain.removeOrphansOfParent(parent).isEmpty());
    }

    @Test
    void removeSingleOrphanOfParent() {
        List<IndexedBlock> blocks = Simulation.randomIndexedBlockChainGenerator(2);
        IndexedBlock orphan =  blocks.get(0);

        blockchain.addOrphan(orphan);
        blockchain.addOrphan(blocks.get(1));

        Set<IndexedBlock> orphans = blockchain.removeOrphansOfParent(orphan.getBlockInfo().getPreviousBlockHash());

        assertEquals(1, orphans.size());
        assertEquals(orphan.getHash(), orphans.stream().findFirst().get().getHash());
    }

    @Test
    void removeMultipleOrphansOfParent() {
        IndexedBlock block1 = Simulation.randomIndexedBlockChainGenerator(1).get(0);
        Hash parent = block1.getBlockInfo().getPreviousBlockHash();
        BlockInfo newBlockInfo = new BlockInfo(
            parent,
            block1.getBlockInfo().getMerkleRoot(),
            block1.getBlockInfo().getTargetValue(),
            block1.getBlockInfo().getNonce(),
            0,
            0,
            block1.getBlockInfo().getTransactionCount(),
            true,
            0,0,0,0,0
        );

        IndexedBlock block2 = new IndexedBlock(Simulation.randomHash(), newBlockInfo);

        blockchain.addOrphan(block1);
        blockchain.addOrphan(block2);

        Set<IndexedBlock> orphans = blockchain.removeOrphansOfParent(parent);
        assertEquals(2, orphans.size());
    }

    @Test
    void popTopBlockEmpty() {
        assertNull(blockchain.popTopBlock());
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
