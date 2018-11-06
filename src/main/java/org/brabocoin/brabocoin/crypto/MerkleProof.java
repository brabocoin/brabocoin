package org.brabocoin.brabocoin.crypto;

import org.brabocoin.brabocoin.model.Hash;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author Sten Wessel
 */
public class MerkleProof {

    private final @NotNull List<Step> steps;

    public MerkleProof(@NotNull List<Step> steps) {
        this.steps = new ArrayList<>(steps);
    }

    public @NotNull List<Step> getSteps() {
        return Collections.unmodifiableList(steps);
    }

    public static class Step {
        private @NotNull Hash hash;
        private @NotNull Side side;

        public static @NotNull Step left(@NotNull Hash hash) {
            return new Step(hash, Side.LEFT);
        }

        public static @NotNull Step right(@NotNull Hash hash) {
            return new Step(hash, Side.RIGHT);
        }

        private Step(@NotNull Hash hash, @NotNull Side side) {
            this.hash = hash;
            this.side = side;
        }

        public @NotNull Hash getHash() {
            return hash;
        }

        public @NotNull Side getSide() {
            return side;
        }
    }

    public enum Side { LEFT, RIGHT }
}
