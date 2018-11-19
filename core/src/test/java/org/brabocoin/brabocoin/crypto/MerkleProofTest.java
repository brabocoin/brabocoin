package org.brabocoin.brabocoin.crypto;

import org.brabocoin.brabocoin.model.Hash;
import org.brabocoin.brabocoin.testutil.Simulation;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test {@link MerkleProof}.
 */
class MerkleProofTest {

    private static final Function<Hash, Hash> doubleSHA =
        hash -> Hashing.digestSHA256(Hashing.digestSHA256(
        hash));

    @ParameterizedTest(name = "Tree with {0} leaves, apply proof to leaf with index {1}")
    @CsvSource({
        "1, 0",
        "5, 0", "5, 4",
        "6, 0", "6, 1", "6, 4", "6, 5",
        "7, 0", "7, 2", "7, 4", "7, 5", "7, 6",
        "8, 0", "8, 1", "8, 2", "8, 6", "8, 7",
        "13, 7"
    })
    void apply(int numberOfLeaves, int verifyIndex) {
        List<Hash> leaves = IntStream.range(0, numberOfLeaves)
            .mapToObj(i -> Simulation.randomHash())
            .collect(Collectors.toList());

        MerkleTree tree = new MerkleTree(doubleSHA, leaves);
        Hash leaf = leaves.get(verifyIndex);

        MerkleProof proof = tree.findProof(leaf);

        Hash producedRoot = proof.apply(leaf, doubleSHA);

        assertEquals(tree.getRoot(), producedRoot);
    }

    @Test
    void applyInvalidRoot() {
        List<Hash> leaves = IntStream.range(0, 13)
            .mapToObj(i -> Simulation.randomHash())
            .collect(Collectors.toList());

        MerkleTree tree = new MerkleTree(doubleSHA, leaves);
        Hash leaf = leaves.get(7);

        MerkleProof proof = tree.findProof(leaf);

        proof.apply(leaf, doubleSHA);

        assertNotEquals(tree.getRoot(), Simulation.randomHash());
    }

    @Test
    void producesRoot() {
        List<Hash> leaves = IntStream.range(0, 13)
            .mapToObj(i -> Simulation.randomHash())
            .collect(Collectors.toList());

        MerkleTree tree = new MerkleTree(doubleSHA, leaves);
        Hash leaf = leaves.get(7);

        MerkleProof proof = tree.findProof(leaf);

        assertTrue(proof.producesRoot(leaf, doubleSHA, tree.getRoot()));
    }

    @Test
    void producesInvalidRoot() {
        List<Hash> leaves = IntStream.range(0, 13)
            .mapToObj(i -> Simulation.randomHash())
            .collect(Collectors.toList());

        MerkleTree tree = new MerkleTree(doubleSHA, leaves);
        Hash leaf = leaves.get(7);

        MerkleProof proof = tree.findProof(leaf);

        assertFalse(proof.producesRoot(leaf, doubleSHA, Simulation.randomHash()));
    }
}
