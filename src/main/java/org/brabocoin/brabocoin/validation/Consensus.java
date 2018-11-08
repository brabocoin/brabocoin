package org.brabocoin.brabocoin.validation;

import com.google.protobuf.ByteString;
import org.brabocoin.brabocoin.Constants;
import org.brabocoin.brabocoin.chain.IndexedBlock;
import org.brabocoin.brabocoin.model.Block;
import org.brabocoin.brabocoin.model.Hash;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;

/**
 * Consensus rules.
 */
public class Consensus {
    // The max block size, excluding the nonce.
    private static final long MAX_BLOCK_SIZE = 1000; // In bytes
    // The max nonce size.
    private static final long MAX_NONCE_SIZE = 16; // In bytes
    // Block maturity depth.
    private static final int BLOCK_MATURITY_DEPTH = 100;
    // Max transaction value range.
    private static final long MAX_TRANSACTION_RANGE = (long) (3 * 10E8);

    private final @NotNull Block genesisBlock = new Block(
        new Hash(ByteString.EMPTY),
        new Hash(ByteString.copyFromUtf8("root")), // TODO: Merkle root needs implementation
        new Hash(ByteString.copyFromUtf8("easy")), // TODO: Determine target value
        ByteString.copyFromUtf8("genesis"),
        0, // TODO: Determine genesis block timestamp
        0,
        Collections.emptyList()
    );

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

    /**
     * Determine amount of Brabocoin in an coinbase output.
     *
     * @return Amount in miniBrabo's
     */
    public long getCoinbaseOutputAmount() {
        return Constants.COIN * 10;
    }

    public @NotNull Block getGenesisBlock() {
        return genesisBlock;
    }

    public @NotNull long getMaxBlockSize() { return MAX_BLOCK_SIZE; }

    public @NotNull long getMaxNonceSize() { return MAX_NONCE_SIZE; }

    public @NotNull int getBlockMaturityDepth() { return BLOCK_MATURITY_DEPTH; }

    public @NotNull long getMaxTransactionRange() { return MAX_TRANSACTION_RANGE; }

}
