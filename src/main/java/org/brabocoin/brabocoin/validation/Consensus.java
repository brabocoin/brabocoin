package org.brabocoin.brabocoin.validation;

import com.google.protobuf.ByteString;
import org.brabocoin.brabocoin.chain.IndexedBlock;
import org.brabocoin.brabocoin.model.Block;
import org.brabocoin.brabocoin.model.Hash;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;

/**
 * Consensus rules.
 */
public class Consensus {

    /**
     * Find the best block from the given collection of blocks.
     *
     * @param blocks
     *     The blocks to compare.
     * @return The best block, or {@code null} if the given collection contained no blocks.
     */
    public @Nullable IndexedBlock bestBlock(@NotNull Collection<IndexedBlock> blocks) {
        // TODO: change to earliest time received instead of time stamp for tiebreaker
        // TODO: when loading from disk, use different tiebreaker (hashCode or similar)
        return blocks.stream()
            .max(Comparator.<IndexedBlock>comparingInt(b -> b.getBlockInfo()
                .getBlockHeight()).thenComparing(Comparator.<IndexedBlock>comparingLong(b -> b.getBlockInfo()
                .getTimestamp()).reversed()))
            .orElse(null);
    }

    public @NotNull Block getGenesisBlock() {
        return new Block(
            new Hash(ByteString.copyFromUtf8("brabo")),
            new Hash(ByteString.copyFromUtf8("root")),
            new Hash(ByteString.copyFromUtf8("easy")),
            ByteString.copyFromUtf8("blabla"),
            0,
            0,
            new ArrayList<>()
        );
    }

}
