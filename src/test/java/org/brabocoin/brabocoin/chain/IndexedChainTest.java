package org.brabocoin.brabocoin.chain;

import org.brabocoin.brabocoin.testutil.Simulation;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test the indexed chain.
 */
class IndexedChainTest {

    private IndexedChain chain;
    private static IndexedBlock genesis;

    @BeforeAll
    static void setUpEnvironment() {
        genesis = Simulation.randomIndexedBlockChainGenerator(1).get(0);
    }

    @BeforeEach
    void setUp() {
        chain = new IndexedChain(genesis);
    }

    @Test
    void getGenesisBlockInit() {
        assertEquals(genesis.getHash(), chain.getGenesisBlock().getHash());
    }

    @Test
    void getGenesisBlockMultiple() {
        IndexedBlock indexedBlock = Simulation.randomIndexedBlockChainGenerator(1, genesis.getHash(), 1).get(0);
        chain.pushTopBlock(indexedBlock);

        IndexedBlock fromChain = chain.getGenesisBlock();

        assertEquals(genesis.getHash(), fromChain.getHash());
    }

    @Test
    void getTopBlockInit() {
        assertEquals(genesis.getHash(), chain.getTopBlock().getHash());
    }

    @Test
    void pushSingleTopBlock() {
        IndexedBlock indexedBlock = Simulation.randomIndexedBlockChainGenerator(1, genesis.getHash(), 1).get(0);
        chain.pushTopBlock(indexedBlock);

        IndexedBlock fromChain = chain.getTopBlock();

        assertNotNull(fromChain);
        assertEquals(indexedBlock.getHash(), fromChain.getHash());
    }

    @Test
    void getTopBlockMultiple() {
        List<IndexedBlock> list = Simulation.randomIndexedBlockChainGenerator(2, genesis.getHash(), 1);
        IndexedBlock last = list.get(1);
        chain.pushTopBlock(list.get(0));
        chain.pushTopBlock(last);

        IndexedBlock fromChain = chain.getTopBlock();

        assertNotNull(fromChain);
        assertEquals(last.getHash(), fromChain.getHash());
    }

    @Test
    void getNextBlockNonExistent() {
        IndexedBlock indexedBlock = Simulation.randomIndexedBlockChainGenerator(1, genesis.getHash(), 1).get(0);
        assertNull(chain.getNextBlock(indexedBlock));
    }

    @Test
    void getNextBlockAfterTop() {
        IndexedBlock indexedBlock = Simulation.randomIndexedBlockChainGenerator(1, genesis.getHash(), 1).get(0);
        chain.pushTopBlock(indexedBlock);
        assertNull(chain.getNextBlock(indexedBlock));
    }

    @Test
    void getNextBlockValid() {
        List<IndexedBlock> list = Simulation.randomIndexedBlockChainGenerator(2, genesis.getHash(), 1);
        IndexedBlock first = list.get(0);
        IndexedBlock last = list.get(1);
        chain.pushTopBlock(first);
        chain.pushTopBlock(last);

        IndexedBlock fromChain = chain.getNextBlock(first);
        assertNotNull(fromChain);
        assertEquals(last.getHash(), fromChain.getHash());
    }

    @Test
    void contains() {
        IndexedBlock indexedBlock = Simulation.randomIndexedBlockChainGenerator(1, genesis.getHash(), 1).get(0);
        chain.pushTopBlock(indexedBlock);
        assertTrue(chain.contains(indexedBlock));
    }

    @Test
    void containsNot() {
        IndexedBlock indexedBlock = Simulation.randomIndexedBlockChainGenerator(1, genesis.getHash(), 1).get(0);
        assertFalse(chain.contains(indexedBlock));
    }

    @Test
    void getBlockAtHeightGenesis() {
        IndexedBlock fromChain = chain.getBlockAtHeight(0);

        assertNotNull(fromChain);
        assertEquals(genesis.getHash(), fromChain.getHash());
    }

    @Test
    void getBlockAtHeight() {
        IndexedBlock indexedBlock = Simulation.randomIndexedBlockChainGenerator(1, genesis.getHash(), 1).get(0);
        chain.pushTopBlock(indexedBlock);

        IndexedBlock fromChain = chain.getBlockAtHeight(1);

        assertNotNull(fromChain);
        assertEquals(indexedBlock.getHash(), fromChain.getHash());
    }

    @Test
    void getBlockAtHeightInvalid() {
        IndexedBlock indexedBlock = Simulation.randomIndexedBlockChainGenerator(1, genesis.getHash(), 1).get(0);
        chain.pushTopBlock(indexedBlock);

        assertNull(chain.getBlockAtHeight(-1));
        assertNull(chain.getBlockAtHeight(2));
    }

    @Test
    void getHeightInit() {
        assertEquals(0, chain.getHeight());
    }

    @Test
    void getHeight() {
        List<IndexedBlock> list = Simulation.randomIndexedBlockChainGenerator(2, genesis.getHash(), 1);
        IndexedBlock first = list.get(0);
        IndexedBlock last = list.get(1);
        chain.pushTopBlock(first);
        assertEquals(1, chain.getHeight());

        chain.pushTopBlock(last);
        assertEquals(2, chain.getHeight());
    }

    @Test
    void popTopBlockGenesis() {
        assertThrows(IllegalStateException.class, () -> chain.popTopBlock());
        assertEquals(genesis.getHash(), chain.getTopBlock().getHash());
    }

    @Test
    void popTopBlock() {
        IndexedBlock indexedBlock = Simulation.randomIndexedBlockChainGenerator(1, genesis.getHash(), 1).get(0);
        chain.pushTopBlock(indexedBlock);

        IndexedBlock fromChain = chain.popTopBlock();
        assertNotNull(fromChain);
        assertEquals(indexedBlock.getHash(), fromChain.getHash());

        assertEquals(genesis.getHash(), chain.getTopBlock().getHash());
    }
}
