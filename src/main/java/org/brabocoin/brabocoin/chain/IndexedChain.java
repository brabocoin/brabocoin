package org.brabocoin.brabocoin.chain;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * A linear chain of blocks (no forks) as part of the blockchain.
 * <p>
 * Note: the genesis (first) block in the chain has height 0.
 */
public class IndexedChain {

    private final @NotNull List<IndexedBlock> chain;

    /**
     * Creates a new empty chain of blocks.
     */
    public IndexedChain() {
        this.chain = new ArrayList<>();
    }

    /**
     * Get the genesis (first) block in this chain.
     *
     * @return The genesis block, or {@code null} if the chain is empty.
     */
    public @Nullable IndexedBlock getGenesisBlock() {
        return this.chain.isEmpty() ? null : this.chain.get(0);
    }

    /**
     * Get the top (highest) block in this chain.
     *
     * @return The top block, or {@code null} if the chain is empty.
     */
    public @Nullable IndexedBlock getTopBlock() {
        return this.chain.isEmpty() ? null : this.chain.get(this.chain.size() - 1);
    }

    /**
     * Get the successor block of the given block in this chain.
     *
     * @param block
     *     The block of which to retrieve the successor of.
     * @return The successor of the given block, or {@code null} no successor block is found.
     */
    public @Nullable IndexedBlock getNextBlock(@NotNull IndexedBlock block) {
        return contains(block) ? getBlockAtHeight(block.getBlockInfo().getBlockHeight() + 1) : null;
    }

    /**
     * Check whether the block is contained on the chain.
     *
     * @param block
     *     The block to check.
     * @return Whether the block is present on the chain.
     */
    public boolean contains(@NotNull IndexedBlock block) {
        return block.equals(getBlockAtHeight(block.getBlockInfo().getBlockHeight()));
    }

    /**
     * Get the block at the specified height in this chain.
     *
     * @param height
     *     The height of the requested block.
     * @return The block at the specified height, or {@code null} if no block at such height
     * exists in this chain.
     */
    public @Nullable IndexedBlock getBlockAtHeight(int height) {
        if (height < 0 || height > this.chain.size()) {
            return null;
        }

        return this.chain.get(height);
    }

    /**
     * Get the height of the top (highest) block in this chain.
     *
     * @return The height of the top (highest) block in the chain, or {@code -1} if the chain is
     * empty.
     */
    public int getHeight() {
        return this.chain.size() - 1;
    }

    /**
     * Removes and returns the top of the chain.
     *
     * @return The top block of the chain, or {@code null} if the chain was empty.
     */
    @Nullable IndexedBlock popTopBlock() {
        return this.chain.remove(this.chain.size() - 1);
    }

    /**
     * Adds the given block as top to the chain.
     *
     * @param block
     *     The block to add.
     */
    void pushTopBlock(@NotNull IndexedBlock block) {
        this.chain.add(block);
    }
}
