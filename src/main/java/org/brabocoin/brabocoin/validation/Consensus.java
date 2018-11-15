package org.brabocoin.brabocoin.validation;

import com.google.protobuf.ByteString;
import org.brabocoin.brabocoin.Constants;
import org.brabocoin.brabocoin.chain.IndexedBlock;
import org.brabocoin.brabocoin.crypto.Hashing;
import org.brabocoin.brabocoin.model.Block;
import org.brabocoin.brabocoin.model.Hash;
import org.brabocoin.brabocoin.util.BigIntegerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.math.BigInteger;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.function.Function;

/**
 * Consensus rules.
 */
public class Consensus {
    /**
     * The max block size, excluding the nonce.
     */
    private static final long MAX_BLOCK_SIZE = 1_000_000L; // In bytes
    /**
     * The max nonce size.
     */
    private static final int MAX_NONCE_SIZE = 16; // In bytes
    /**
     * Block maturity depth.
     */
    private static final int COINBASE_MATURITY_DEPTH = 100;
    /**
     * Max money value.
     */
    private static final long MAX_MONEY_VALUE = (long) (3E9);
    /**
     * Constant target value.
     */
    private static final Hash TARGET_VALUE = new Hash(
            ByteString.copyFrom(
                    BigInteger.valueOf(257).multiply(BigInteger.TEN.pow(67)).toByteArray()
            )
    );
    /**
     * Double SHA256 hash.
     */
    private static final Function<Hash, Hash> DOUBLE_SHA = (h) -> Hashing.digestSHA256(Hashing.digestSHA256(h));

    private final @NotNull Block GENESIS_BLOCK = new Block(
            new Hash(ByteString.EMPTY),
            new Hash(ByteString.copyFromUtf8("root")), // TODO: Merkle root needs implementation
            new Hash(ByteString.copyFromUtf8("easy")), // TODO: Determine target value
            BigInteger.ZERO,
            0,
            Collections.emptyList()
    );

    /**
     * Find the best valid block from the given collection of blocks.
     *
     * @param blocks The blocks to compare.
     * @return The best block, or {@code null} if the given collection contained no valid blocks.
     */
    public @Nullable IndexedBlock bestValidBlock(@NotNull Collection<IndexedBlock> blocks) {
        return blocks.stream()
                .filter(i -> i.getBlockInfo().isValid())
            .max(Comparator.<IndexedBlock>comparingInt(b -> b.getBlockInfo()
                        .getBlockHeight()).thenComparing(Comparator.comparing(IndexedBlock::getHash).reversed()))
                .orElse(null);
    }

    /**
     * Determine amount of Brabocoin in a coinbase output.
     *
     * @return Amount in miniBrabo's
     */
    public long getBlockReward() {
        return Constants.COIN * 10;
    }

    public @NotNull Block getGenesisBlock() {
        return GENESIS_BLOCK;
    }

    public @NotNull long getMaxBlockSize() {
        return MAX_BLOCK_SIZE;
    }

    public @NotNull int getMaxNonceSize() {
        return MAX_NONCE_SIZE;
    }

    public @NotNull BigInteger getMaxNonce() {
        return BigIntegerUtil.getMaxBigInteger(getMaxNonceSize());
    }

    public @NotNull int getCoinbaseMaturityDepth() {
        return COINBASE_MATURITY_DEPTH;
    }

    public @NotNull long getMaxMoneyValue() {
        return MAX_MONEY_VALUE;
    }

    public @NotNull Hash getTargetValue() {
        return TARGET_VALUE;
    }

    public @NotNull Function<Hash, Hash> getMerkleTreeHashFunction()
    {
        return DOUBLE_SHA;
    }
}
