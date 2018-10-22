package org.brabocoin.brabocoin.chain;

import org.brabocoin.brabocoin.testutil.Simulation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test the indexed chain.
 */
class IndexedChainTest {

    private IndexedChain chain;

    @BeforeEach
    void setUp() {
        chain = new IndexedChain();
    }

    @Test
    void getGenesisBlockEmptyChain() {
        assertNull(chain.getGenesisBlock());
    }

    @Test
    void getGenesisBlockSingleBlock() {
        IndexedBlock indexedBlock = Simulation.randomIndexedBlockChainGenerator(1).get(0);
        chain.pushTopBlock(indexedBlock);

        IndexedBlock fromChain = chain.getGenesisBlock();

        assertNotNull(fromChain);
        assertEquals(indexedBlock.getHash(), fromChain.getHash());
    }

    @Test
    void getGenesisBlockMultiple() {
        List<IndexedBlock> list = Simulation.randomIndexedBlockChainGenerator(2);
        IndexedBlock first = list.get(0);
        chain.pushTopBlock(first);
        chain.pushTopBlock(list.get(1));

        IndexedBlock fromChain = chain.getGenesisBlock();

        assertNotNull(fromChain);
        assertEquals(first.getHash(), fromChain.getHash());
    }

    @Test
    void getTopBlockEmptyChain() {
        assertNull(chain.getTopBlock());
    }

    @Test
    void pushSingleTopBlock() {
        IndexedBlock indexedBlock = Simulation.randomIndexedBlockChainGenerator(1).get(0);
        chain.pushTopBlock(indexedBlock);

        IndexedBlock fromChain = chain.getTopBlock();

        assertNotNull(fromChain);
        assertEquals(indexedBlock.getHash(), fromChain.getHash());
    }

    @Test
    void getTopBlockMultiple() {
        List<IndexedBlock> list = Simulation.randomIndexedBlockChainGenerator(2);
        IndexedBlock last = list.get(1);
        chain.pushTopBlock(list.get(0));
        chain.pushTopBlock(last);

        IndexedBlock fromChain = chain.getTopBlock();

        assertNotNull(fromChain);
        assertEquals(last.getHash(), fromChain.getHash());
    }

    @Test
    void getNextBlockNonExistent() {
        IndexedBlock indexedBlock = Simulation.randomIndexedBlockChainGenerator(1).get(0);
        assertNull(chain.getNextBlock(indexedBlock));
    }

    @Test
    void getNextBlockAfterTop() {
        IndexedBlock indexedBlock = Simulation.randomIndexedBlockChainGenerator(1).get(0);
        chain.pushTopBlock(indexedBlock);
        assertNull(chain.getNextBlock(indexedBlock));
    }

    @Test
    void getNextBlockValid() {
        List<IndexedBlock> list = Simulation.randomIndexedBlockChainGenerator(2);
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
        IndexedBlock indexedBlock = Simulation.randomIndexedBlockChainGenerator(1).get(0);
        chain.pushTopBlock(indexedBlock);
        assertTrue(chain.contains(indexedBlock));
    }

    @Test
    void containsNot() {
        IndexedBlock indexedBlock = Simulation.randomIndexedBlockChainGenerator(1).get(0);
        assertFalse(chain.contains(indexedBlock));
    }

    @Test
    void getBlockAtHeight() {
        IndexedBlock indexedBlock = Simulation.randomIndexedBlockChainGenerator(1).get(0);
        chain.pushTopBlock(indexedBlock);

        IndexedBlock fromChain = chain.getBlockAtHeight(0);

        assertNotNull(fromChain);
        assertEquals(indexedBlock.getHash(), fromChain.getHash());
    }

    @Test
    void getBlockAtHeightInvalid() {
        assertNull(chain.getBlockAtHeight(0));

        IndexedBlock indexedBlock = Simulation.randomIndexedBlockChainGenerator(1).get(0);
        chain.pushTopBlock(indexedBlock);

        assertNull(chain.getBlockAtHeight(-1));
        assertNull(chain.getBlockAtHeight(1));
    }

    @Test
    void getHeightEmpty() {
        assertEquals(-1, chain.getHeight());
    }

    @Test
    void getHeight() {
        List<IndexedBlock> list = Simulation.randomIndexedBlockChainGenerator(2);
        IndexedBlock first = list.get(0);
        IndexedBlock last = list.get(1);
        chain.pushTopBlock(first);
        assertEquals(0, chain.getHeight());

        chain.pushTopBlock(last);
        assertEquals(1, chain.getHeight());
    }

    @Test
    void popTopBlockEmpty() {
        assertNull(chain.popTopBlock());
    }

    @Test
    void popTopBlockReturns() {
        IndexedBlock indexedBlock = Simulation.randomIndexedBlockChainGenerator(1).get(0);
        chain.pushTopBlock(indexedBlock);

        IndexedBlock fromChain = chain.popTopBlock();
        assertNotNull(fromChain);
        assertEquals(indexedBlock.getHash(), fromChain.getHash());

        assertNull(chain.getTopBlock());
    }

    @Test
    void popTopBlockRemoves() {
        IndexedBlock indexedBlock = Simulation.randomIndexedBlockChainGenerator(1).get(0);
        chain.pushTopBlock(indexedBlock);
        chain.popTopBlock();

        assertNull(chain.getTopBlock());
    }
}
