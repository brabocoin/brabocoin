package org.brabocoin.brabocoin.validation;

import com.google.protobuf.ByteString;
import org.brabocoin.brabocoin.chain.IndexedBlock;
import org.brabocoin.brabocoin.crypto.EllipticCurve;
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
    private static final long MAX_MONEY_VALUE = (long)(3E9);

    /**
     * Constant target value.
     */
    private static final Hash TARGET_VALUE = new Hash(
        ByteString.copyFrom(
            BigInteger.valueOf(3216).multiply(BigInteger.TEN.pow(65)).toByteArray()
        )
    );

    /**
     * Double SHA256 hash.
     */
    private static final Function<Hash, Hash> DOUBLE_SHA =
        (h) -> Hashing.digestSHA256(Hashing.digestSHA256(h));

    /**
     * The max nonce value.
     */
    private static final BigInteger MAX_NONCE = BigIntegerUtil.getMaxBigInteger(MAX_NONCE_SIZE);

    /**
     * The amount of brabocents that equals one brabocoin.
     */
    public static final long COIN = 100;

    /**
     * The block reward value.
     */
    private static final long BLOCK_REWARD = COIN * 10;

    /**
     * Minimum transaction fee in brabocents.
     */
    private static final long MINIMUM_TRANSACTION_FEE = 1;

    private static final int MAX_BLOCK_HEADER_SIZE = 139;

    private static final int MAX_COINBASE_TRANSACTION_SIZE = 36;

    /**
     * Elliptic curve.
     */
    private static final EllipticCurve CURVE = EllipticCurve.secp256k1();

    private static final @NotNull Block GENESIS_BLOCK = new Block(
        new Hash(ByteString.EMPTY),
        new Hash(ByteString.EMPTY),
        new Hash(ByteString.EMPTY),
        BigInteger.ZERO,
        0,
        Collections.emptyList(),
        0
    );

    /**
     * Find the best valid block from the given collection of blocks.
     *
     * @param blocks
     *     The blocks to compare.
     * @return The best block, or {@code null} if the given collection contained no valid blocks.
     */
    public @Nullable IndexedBlock bestValidBlock(@NotNull Collection<IndexedBlock> blocks) {
        return blocks.stream()
            .filter(i -> i.getBlockInfo().isValid())
            .max(Comparator.<IndexedBlock>comparingInt(b -> b.getBlockInfo()
                .getBlockHeight()).thenComparing(Comparator.comparing(IndexedBlock::getHash)
                .reversed()))
            .orElse(null);
    }

    /**
     * Determine amount of Brabocoin in a coinbase output.
     *
     * @return Amount in brabocents
     */
    public long getBlockReward() {
        return BLOCK_REWARD;
    }

    public @NotNull Block getGenesisBlock() {
        return GENESIS_BLOCK;
    }

    public long getMaxBlockSize() {
        return MAX_BLOCK_SIZE;
    }

    public int getMaxNonceSize() {
        return MAX_NONCE_SIZE;
    }

    public @NotNull BigInteger getMaxNonce() {
        return MAX_NONCE;
    }

    public int getCoinbaseMaturityDepth() {
        return COINBASE_MATURITY_DEPTH;
    }

    public long getMaxMoneyValue() {
        return MAX_MONEY_VALUE;
    }

    public @NotNull Hash getTargetValue() {
        return TARGET_VALUE;
    }

    public @NotNull Function<Hash, Hash> getMerkleTreeHashFunction() {
        return DOUBLE_SHA;
    }

    public int getMaxBlockHeaderSize() {
        return MAX_BLOCK_HEADER_SIZE;
    }

    public int getMaxCoinbaseTransactionSize() {
        return MAX_COINBASE_TRANSACTION_SIZE;
    }

    /**
     * Minimum transaction fee in brabocents.
     *
     * @return Minimum transaction fee.
     */
    public long getMinimumTransactionFee() {
        return MINIMUM_TRANSACTION_FEE;
    }

    public @NotNull EllipticCurve getCurve() {
        return CURVE;
    }

    public Long getMaxTransactionSize() {
        return getMaxBlockSize()
            + getMaxNonceSize()
            - getMaxBlockHeaderSize()
            - getMaxCoinbaseTransactionSize()
            - Long.BYTES;
    }
}
