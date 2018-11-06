package org.brabocoin.brabocoin.crypto;

import org.brabocoin.brabocoin.model.Hash;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.text.MessageFormat;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.function.Function;
import java.util.logging.Logger;

import static org.brabocoin.brabocoin.util.ByteUtil.toHexString;

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

    /**
     * Root of the Merkle tree. Only stored once requested.
     */
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
            LOGGER.warning("Merkle tree was supplied zero leaves.");
            throw new IllegalArgumentException("At least one leaf must be present.");
        }

        this.hashingFunction = hashingFunction;
        this.leaves = new ArrayList<>(leaves);

        LOGGER.fine(() -> MessageFormat.format("Initialized Merkle tree with {0} leaves.",
            this.leaves.size()
        ));
    }

    /**
     * Get the root of the Merkle tree.
     * <p>
     * When the root has not yet been calculated, it is calculated when this method is called. It
     * is cached afterwards such that repeated calls to this method are not unnecessarily expensive.
     *
     * @return The root of the Merkle tree.
     */
    public @NotNull Hash getRoot() {
        if (root == null) {
            LOGGER.fine("Root requested but not found, calculating.");
            computeProofToRoot(null);
        }

        return root;
    }

    /**
     * Computes a Merkle proof for the given leaf hash.
     * <p>
     * To compute the proof, all internal nodes must be calculated, including the root. As a
     * consequence, this method can be used to compute the root of the tree. When the proof path
     * is not important, {@code null} can be given as argument. In this case, the returned proof
     * will be empty.
     * <p>
     * When the root is not already known, the value will be set when the root is calculated by
     * this method.
     *
     * @param leaf
     *     The leaf element to find a proof for.
     * @return The proof showing that the given leaf is in the tree, or {@code null} of the
     * supplied leaf is {@code null}.
     */
    @Contract("!null -> !null; null -> null")
    private @Nullable MerkleProof computeProofToRoot(@Nullable Hash leaf) {
        LOGGER.fine("Computing proof for leaf.");

        // Working queue of nodes to process
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

            LOGGER.finest(() -> MessageFormat.format("Hash of next inner node: {0}",
                toHexString(innerNode.getValue())
            ));

            // Check if either child is the desired path
            if (leftChild.equals(onPath)) {
                LOGGER.finest("Left child is on the proof path, add right sibling to proof.");
                proof.add(MerkleProof.Step.right(rightChild));
                onPath = innerNode;
            }
            else if (rightChild.equals(onPath)) {
                LOGGER.finest("Right child is on the proof path, add left sibling to proof.");
                proof.add(MerkleProof.Step.left(leftChild));
                onPath = innerNode;
            }
        }

        // There is now only one node left in the queue, which is the root node
        if (root == null) {
            root = nodes.remove();
            LOGGER.finest(() -> MessageFormat.format(
                "Setting the root of the tree: {0}",
                toHexString(root != null ? root.getValue() : null)
            ));
        }

        return leaf == null ? null : new MerkleProof(proof);
    }

    /**
     * Find a proof path that proves that the given hash is in the Merkle tree.
     *
     * @param hash
     *     The hash to find a proof for.
     * @return The proof that shows that the hash is in the tree.
     * @throws IllegalArgumentException
     *     When the given hash is not a leaf in the Merkle tree.
     */
    public @NotNull MerkleProof findProof(@NotNull Hash hash) {
        if (!leaves.contains(hash)) {
            LOGGER.warning("Merkle proof requested for leaf not recorded in the tree.");
            throw new IllegalArgumentException("Provided hash is not recorded in the Merkle tree.");
        }

        return computeProofToRoot(hash);
    }
}
