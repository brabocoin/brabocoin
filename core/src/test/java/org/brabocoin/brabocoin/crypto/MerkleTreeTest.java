package org.brabocoin.brabocoin.crypto;

import org.brabocoin.brabocoin.testutil.Simulation;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests {@link MerkleTree}.
 */
class MerkleTreeTest {

    @Test
    void emptyLeaves() {
        assertThrows(
            IllegalArgumentException.class,
            () -> new MerkleTree(hash -> hash, new ArrayList<>())
        );
    }

    @Test
    void findProofNotInLeaves() {
        MerkleTree tree = new MerkleTree(hash -> hash, Collections.singletonList(Simulation.randomHash()));

        assertThrows(IllegalArgumentException.class, () -> tree.findProof(Simulation.randomHash()));
    }

    @Test
    void getRoot() {
        MerkleTree tree = new MerkleTree(hash -> hash, Collections.singletonList(Simulation.randomHash()));

        assertNotNull(tree.getRoot());
    }
}
