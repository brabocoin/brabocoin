package org.brabocoin.brabocoin.validation;

import org.brabocoin.brabocoin.chain.IndexedBlock;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Comparator;

/**
 * Consensus rules.
 */
public final class Consensus {

    // Private constructor to prevent instantiation
    private Consensus() {

    }

    /**
     * Find the best block from the given collection of blocks.
     *
     * @param blocks
     *     The blocks to compare.
     * @return The best block, or {@code null} if the given collection contained no blocks.
     */
    public static @Nullable IndexedBlock bestBlock(@NotNull Collection<IndexedBlock> blocks) {
        // TODO: change to earliest time received instead of time stamp for tiebreaker
        // TODO: when loading from disk, use different tiebreaker (hashCode or similar)
        return blocks.stream()
            .max(Comparator.<IndexedBlock>comparingInt(b -> b.getBlockInfo()
                .getBlockHeight()).thenComparing(Comparator.<IndexedBlock>comparingLong(b -> b.getBlockInfo()
                .getTimestamp()).reversed()))
            .orElse(null);
    }

}
