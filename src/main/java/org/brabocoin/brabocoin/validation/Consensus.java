package org.brabocoin.brabocoin.validation;

import com.google.protobuf.ByteString;
import org.brabocoin.brabocoin.Constants;
import org.brabocoin.brabocoin.chain.IndexedBlock;
import org.brabocoin.brabocoin.model.Block;
import org.brabocoin.brabocoin.model.Hash;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.math.BigInteger;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;

/**
 * Consensus rules.
 */
public class Consensus {
    /**
     * The max block size, excluding the nonce.
     */
    private static final long MAX_BLOCK_SIZE = 1000000; // In bytes
    /**
     * The max nonce size.
     */
    private static final long MAX_NONCE_SIZE = 16; // In bytes
    /**
     * Block maturity depth.
     */
    private static final int COINBASE_MATURITY_DEPTH = 100;
    /**
     * Max transaction value range.
     */
    private static final long MAX_TRANSACTION_RANGE = (long) (3E9);
    /**
     * Constant target value.
     */
    private static final Hash TARGET_VALUE = new Hash(
            ByteString.copyFrom(
                    BigInteger.valueOf(257).multiply(BigInteger.TEN.pow(67)).toByteArray()
            )
    );

    private final @NotNull Block genesisBlock = new Block(
        new Hash(ByteString.EMPTY),
        new Hash(ByteString.copyFromUtf8("root")), // TODO: Merkle root needs implementation
        new Hash(ByteString.copyFromUtf8("easy")), // TODO: Determine target value
        ByteString.copyFromUtf8("genesis"),
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
        return blocks.stream()
            .max(Comparator.<IndexedBlock>comparingInt(b -> b.getBlockInfo()
                .getBlockHeight()).thenComparing(Comparator.comparing(IndexedBlock::getHash).reversed()))
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

    public @NotNull int getCoinbaseMaturityDepth() { return COINBASE_MATURITY_DEPTH; }

    public @NotNull long getMaxTransactionRange() { return MAX_TRANSACTION_RANGE; }

    public @NotNull Hash getTargetValue() { return TARGET_VALUE; }

}
