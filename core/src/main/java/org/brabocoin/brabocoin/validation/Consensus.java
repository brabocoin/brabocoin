package org.brabocoin.brabocoin.validation;

import com.google.protobuf.ByteString;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.LongProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import org.bouncycastle.util.encoders.Hex;
import org.brabocoin.brabocoin.chain.IndexedBlock;
import org.brabocoin.brabocoin.crypto.EllipticCurve;
import org.brabocoin.brabocoin.crypto.Hashing;
import org.brabocoin.brabocoin.model.Block;
import org.brabocoin.brabocoin.model.Hash;
import org.brabocoin.brabocoin.model.dal.UnspentOutputInfo;
import org.brabocoin.brabocoin.util.BigIntegerUtil;
import org.brabocoin.brabocoin.util.ByteUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.function.Function;

/**
 * Consensus rules.
 */
public class Consensus {

    public Consensus() {
        maxNonceSize.addListener((observable, oldValue, newValue) -> {
            MAX_NONCE = BigIntegerUtil.getMaxBigInteger(
                newValue.intValue()
            );
        });
        targetValue.addListener((observable, oldValue, newValue) -> {
            try {
                if (newValue.startsWith("0")) {
                    cachedTargetValue = new Hash(ByteString.copyFrom(Hex.decode(newValue)));
                }
                else {
                    cachedTargetValue =
                        new Hash(ByteUtil.toUnsigned(new BigDecimal(newValue).toBigInteger()));
                }
            }
            catch (NumberFormatException e) {
                // ignored
            }
        });
    }

    //================================================================================
    // Configurable
    //================================================================================

    /**
     * The max block size, excluding the nonce.
     */
    private static final long MAX_BLOCK_SIZE = 10_000L; // In bytes

    public LongProperty maxBlockSize = new SimpleLongProperty(MAX_BLOCK_SIZE);

    /**
     * The max nonce size.
     */
    private static final int MAX_NONCE_SIZE = 5; // In bytes

    public IntegerProperty maxNonceSize = new SimpleIntegerProperty(MAX_NONCE_SIZE);

    /**
     * Block maturity depth.
     */
    private static final int COINBASE_MATURITY_DEPTH = 10;

    public IntegerProperty coinbaseMaturityDepth =
        new SimpleIntegerProperty(COINBASE_MATURITY_DEPTH);

    /**
     * The block reward value.
     */
    private static final long BLOCK_REWARD = 1_000L;

    public LongProperty blockReward = new SimpleLongProperty(BLOCK_REWARD);


    /**
     * Minimum transaction fee in brabocents.
     */
    private static final long MINIMUM_TRANSACTION_FEE = 1;

    public LongProperty minimumTransactionFee = new SimpleLongProperty(MINIMUM_TRANSACTION_FEE);

    private static final int MAX_BLOCK_HEADER_SIZE = 139;

    public IntegerProperty maxBlockHeaderSize = new SimpleIntegerProperty(MAX_BLOCK_HEADER_SIZE);

    private static final int MAX_COINBASE_TRANSACTION_SIZE = 36;

    public IntegerProperty maxCoinbaseTransactionSize = new SimpleIntegerProperty(
        MAX_COINBASE_TRANSACTION_SIZE);

    public StringProperty targetValue = new SimpleStringProperty("3216E65");
    private Hash cachedTargetValue =
        new Hash(ByteUtil.toUnsigned(new BigDecimal(targetValue.get()).toBigInteger()));


    //================================================================================
    // Non-configurable
    //================================================================================

    /**
     * Max money value.
     */
    private static final long MAX_MONEY_VALUE = (long)(3E18);

    /**
     * The max nonce value.
     * <p>
     * Note: updates with {@link #maxNonceSize}.
     */
    private static BigInteger MAX_NONCE = BigIntegerUtil.getMaxBigInteger(MAX_NONCE_SIZE);

    /**
     * Double SHA256 hash.
     */
    private static final Function<Hash, Hash> DOUBLE_SHA =
        (h) -> Hashing.digestSHA256(Hashing.digestSHA256(h));

    private static final Function<ByteString, Hash> RIPEMD_SHA =
        (b) -> Hashing.digestRIPEMD160(Hashing.digestSHA256(b));

    /**
     * The amount of brabocents that equals one brabocoin.
     */
    public static final long COIN = 100;
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

    public @NotNull Block getGenesisBlock() {
        return GENESIS_BLOCK;
    }

    public long getMaxMoneyValue() {
        return MAX_MONEY_VALUE;
    }

    public @NotNull BigInteger getMaxNonce() {
        return MAX_NONCE;
    }

    public @NotNull Function<Hash, Hash> getMerkleTreeHashFunction() {
        return DOUBLE_SHA;
    }

    public @NotNull Function<ByteString, Hash> getPublicKeyHashFunction() {
        return RIPEMD_SHA;
    }

    public @NotNull EllipticCurve getCurve() {
        return CURVE;
    }

    public Long getMaxTransactionSize() {
        return maxBlockSize.get()
            - maxBlockHeaderSize.get()
            - maxCoinbaseTransactionSize.get()
            - Long.BYTES;
    }

    public boolean immatureCoinbase(int chainHeight, UnspentOutputInfo info) {
        return info.isCoinbase() && chainHeight - coinbaseMaturityDepth.get() < info.getBlockHeight();
    }

    public long getMaxBlockSize() {
        return maxBlockSize.get();
    }

    public int getMaxNonceSize() {
        return maxNonceSize.get();
    }

    public int getCoinbaseMaturityDepth() {
        return coinbaseMaturityDepth.get();
    }

    public long getBlockReward() {
        return blockReward.get();
    }

    public long getMinimumTransactionFee() {
        return minimumTransactionFee.get();
    }

    public Hash getTargetValue() {
        return cachedTargetValue;
    }

}
