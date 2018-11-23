package org.brabocoin.brabocoin.chain;

import org.brabocoin.brabocoin.model.Hash;
import org.brabocoin.brabocoin.model.dal.BlockInfo;
import org.jetbrains.annotations.NotNull;

import java.util.logging.Logger;

/**
 * Indexed block for memory storage.
 *
 * @see IndexedChain
 */
public class IndexedBlock {

    private static final Logger LOGGER = Logger.getLogger(IndexedBlock.class.getName());

    private final @NotNull Hash hash;

    private final @NotNull BlockInfo blockInfo;

    /**
     * Create a new indexed block for memory storage.
     *
     * @param hash
     *     The hash of the block.
     * @param blockInfo
     *     The associated block info.
     */
    public IndexedBlock(@NotNull Hash hash, @NotNull BlockInfo blockInfo) {
        this.hash = hash;
        this.blockInfo = blockInfo;
    }

    /**
     * Get the hash of the indexed block.
     *
     * @return The block hash.
     */
    public @NotNull Hash getHash() {
        return hash;
    }

    /**
     * Get the block info of the indexed block.
     *
     * @return The block info.
     */
    public @NotNull BlockInfo getBlockInfo() {
        return blockInfo;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        IndexedBlock that = (IndexedBlock)o;

        return hash.equals(that.hash);
    }

    @Override
    public int hashCode() {
        return hash.hashCode();
    }
}
