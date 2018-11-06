package org.brabocoin.brabocoin.crypto;

import org.brabocoin.brabocoin.model.Hash;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.function.Function;
import java.util.logging.Logger;

/**
 * Merkle tree of hashes.
 * <p>
 * The Merkle tree is defined by its leaves and forms a fully balanced binary tree. The interior
 * nodes (including the root node) are defined as {@code H(child1 || child2)}, where {@code
 * ||} denotes the concatenation of hashes and {@code H} is a hashing function.
 * <p>
 * Note that the Merkle tree must have an even number of leaves. If an uneven number of leaves is
 * supplied, the last node is duplicated to fill up the tree.
 */
public class MerkleTree {

    private static final Logger LOGGER = Logger.getLogger(MerkleTree.class.getName());

    /**
     * The hashing function producing the value of the interior nodes.
     */
    private final @NotNull Function<Hash, Hash> hashingFunction;

    /**
     * Leaves of the tree. The number of leaves is always even.
     */
    private final @NotNull List<Hash> leaves;

    private @Nullable Hash root;

    /**
     * Create a new Merkle tree with the provided leaf nodes.
     *
     * @param hashingFunction
     *     The hashing function used to compute the interior nodes.
     * @param leaves
     *     The leaves of the tree.
     * @throws IllegalArgumentException
     *     When the list of leaves is empty.
     */
    public MerkleTree(@NotNull Function<Hash, Hash> hashingFunction, @NotNull List<Hash> leaves) {
        if (leaves.isEmpty()) {
            throw new IllegalArgumentException("At least one leaf must be present.");
        }

        this.hashingFunction = hashingFunction;
        this.leaves = new ArrayList<>(leaves);
    }

    public @NotNull Hash getRoot() {
        if (root == null) {
            computeProofToRoot(null);
        }

        return root;
    }

    private @NotNull MerkleProof computeProofToRoot(@Nullable Hash leaf) {
        Queue<Hash> nodes = new ArrayDeque<>(leaves);
        List<MerkleProof.Step> proof = new ArrayList<>();
        Hash onPath = leaf;

        while (nodes.size() > 1) {
            // Remove the next two nodes from the queue
            Hash leftChild = nodes.remove();
            Hash rightChild = nodes.remove();

            // Compute the next inner node and add to the queue
            Hash innerNode = hashingFunction.apply(leftChild.concat(rightChild));
            nodes.add(innerNode);

            // Check if either child is the desired path
            if (leftChild.equals(onPath)) {
                proof.add(MerkleProof.Step.right(rightChild));
                onPath = innerNode;
            }
            else if (rightChild.equals(onPath)) {
                proof.add(MerkleProof.Step.left(leftChild));
                onPath = innerNode;
            }
        }

        // There is now only one node left in the queue, which is the root node
        root = nodes.remove();

        return new MerkleProof(proof);
    }

    public @NotNull MerkleProof findProof(@NotNull Hash hash) {
        if (!leaves.contains(hash)) {
            throw new IllegalArgumentException("Provided hash is not recorded in the Merkle tree.");
        }

        return computeProofToRoot(hash);
    }
}
