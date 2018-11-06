package org.brabocoin.brabocoin.crypto;

import org.brabocoin.brabocoin.model.Hash;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.logging.Logger;

import static org.brabocoin.brabocoin.util.ByteUtil.toHexString;

/**
 * A Merkle proof describes a list of hashing steps for an initial hash {@code h} that must be
 * followed to obtain the the root of the Merkle tree.
 * <p>
 * When the root that is produced by the proof is equal to the expected root, the proof shows
 * that the initial hash {@code h} is part of the Merkle tree.
 *
 * @see Step
 */
public class MerkleProof {

    private static final Logger LOGGER = Logger.getLogger(MerkleProof.class.getName());

    /**
     * List of hashing steps.
     */
    private final @NotNull List<Step> steps;

    /**
     * Create a new Merkle proof from a list of hashing steps.
     *
     * @param steps
     *     The hashing steps.
     */
    public MerkleProof(@NotNull List<Step> steps) {
        this.steps = new ArrayList<>(steps);
    }

    /**
     * Get the hashing steps.
     *
     * @return The hashing steps.
     */
    public @NotNull List<Step> getSteps() {
        return Collections.unmodifiableList(steps);
    }

    /**
     * Apply the hashing steps in the proof to the given initial {@code hash}.
     * <p>
     * The provided {@code hashingFunction} is used to compute the hashes of the intermediate
     * nodes, and must match the hashing function used in the Merkle tree this proof was
     * constructed from.
     *
     * @param hash
     *     The initial hash to apply the proof to.
     * @param hashingFunction
     *     The hashing function used to compute the value of the intermediate nodes.
     * @return The root of the Merkle tree as produced by this proof.
     */
    public @NotNull Hash apply(@NotNull Hash hash, @NotNull Function<Hash, Hash> hashingFunction) {
        LOGGER.fine("Applying the proof to the given hash.");

        Hash result = hash;
        for (Step step : steps) {
            Hash concat = step.side == Side.LEFT ? step.hash.concat(result) :
                result.concat(step.hash);

            Hash innerNode = hashingFunction.apply(concat);
            LOGGER.finest(() -> MessageFormat.format("Intermediate node: {0}",
                toHexString(innerNode.getValue())
            ));

            result = innerNode;
        }

        return result;
    }

    /**
     * Checks whether this proof, applied to the initial {@code hash}, produces the expected
     * {@code root}.
     * <p>
     * When this evaluates to {@code true}, {@code hash} is proven to be in the Merkle tree with
     * root {@code root}.
     * <p>
     * The provided {@code hashingFunction} is used to compute the hashes of the intermediate
     * nodes, and must match the hashing function used in the Merkle tree this proof was
     * constructed from.
     *
     * @param hash
     *     The initial hash to which the proof is applied.
     * @param hashingFunction
     *     The hashing function used to compute the value of the intermediate nodes.
     * @param root
     *     The expected root of the Merkle tree.
     * @return Whether the proof applied to {@code hash} evaluates to the given {@code root}.
     */
    public boolean producesRoot(@NotNull Hash hash, @NotNull Function<Hash, Hash> hashingFunction
        , @NotNull Hash root) {
        return apply(hash, hashingFunction).equals(root);
    }

    /**
     * Describes a hashing step in the Merkle proof.
     * <p>
     * A hashing step consists of a hash that is concatenated with the current result, either on
     * the left or right side of the current result. The side at which the hash should be
     * concatenated is indicated in the step.
     * <p>
     * When the side is {@link Side#LEFT}, the hash in the step should be concatenated on the
     * left side; {@code stepHash || currentResult}.
     * When the side is {@link Side#RIGHT}, the hash in the step should be concatenated on the
     * right side; {@code currentResult || stepHash}.
     */
    public static class Step {

        /**
         * The hash that is concatenated to the current result.
         */
        private @NotNull Hash hash;

        /**
         * The side at which the hash should be concatenated.
         */
        private @NotNull Side side;

        /**
         * Create a new step with the hash that must be concatenated on the left side.
         * @param hash The hash.
         * @return The Merkle proof step.
         */
        @Contract("_ -> new")
        public static @NotNull Step left(@NotNull Hash hash) {
            return new Step(hash, Side.LEFT);
        }

        /**
         * Create a new step with the hash that must be concatenated on the right side.
         * @param hash The hash.
         * @return The Merkle proof step.
         */
        @Contract("_ -> new")
        public static @NotNull Step right(@NotNull Hash hash) {
            return new Step(hash, Side.RIGHT);
        }

        private Step(@NotNull Hash hash, @NotNull Side side) {
            this.hash = hash;
            this.side = side;
        }

        /**
         * Get the hash that must be concatenated.
         * @return The hash.
         */
        public @NotNull Hash getHash() {
            return hash;
        }

        /**
         * Get the side at which the hash must be concatenated.
         * @return The side.
         */
        public @NotNull Side getSide() {
            return side;
        }
    }

    /**
     * Indicates the side at which a hash in a Merkle proof needs to be concatenated.
     *
     * @see Step
     */
    public enum Side {
        LEFT,
        RIGHT
    }
}
