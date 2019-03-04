package org.brabocoin.brabocoin.validation;

import com.google.protobuf.ByteString;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
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
            Hash newHash = ByteUtil.parseHash(newValue);
            if (newHash != null) {
                cachedTargetValue = newHash;
            }
        });
        initializeValues();
    }

    public void initializeValues() {
        maxBlockSize.setValue(MAX_BLOCK_SIZE);
        maxNonceSize.setValue(MAX_NONCE_SIZE);
        coinbaseMaturityDepth.setValue(COINBASE_MATURITY_DEPTH);
        blockReward.setValue(BLOCK_REWARD);
        minimumTransactionFee.setValue(MINIMUM_TRANSACTION_FEE);
        targetValue.setValue(TARGET_VALUE_STRING);
    }

    //================================================================================
    // Configurable
    //================================================================================

    /**
     * The max block size, excluding the nonce.
     */
    private static final int MAX_BLOCK_SIZE = 10_000; // In bytes

    public IntegerProperty maxBlockSize = new SimpleIntegerProperty();

    /**
     * The max nonce size.
     */
    private static final int MAX_NONCE_SIZE = 5; // In bytes

    public IntegerProperty maxNonceSize = new SimpleIntegerProperty();

    /**
     * Block maturity depth.
     */
    private static final int COINBASE_MATURITY_DEPTH = 10;

    public IntegerProperty coinbaseMaturityDepth =
        new SimpleIntegerProperty();

    /**
     * The block reward value.
     */
    private static final int BLOCK_REWARD = 1_000;

    public IntegerProperty blockReward = new SimpleIntegerProperty();


    /**
     * Minimum transaction fee in brabocents.
     */
    private static final int MINIMUM_TRANSACTION_FEE = 1;

    public IntegerProperty minimumTransactionFee =
        new SimpleIntegerProperty();

    private static final String TARGET_VALUE_STRING = "3216E65";
    public StringProperty targetValue = new SimpleStringProperty();
    private Hash cachedTargetValue;


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
    public static final int COIN = 100;
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

    public @NotNull long getMaxMoneyValue() {
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

    private static final int MAX_COINBASE_TRANSACTION_SIZE = 36;

    private static final int MAX_BLOCK_HEADER_SIZE = 134;

    public int getMaxTransactionSize() {
        return maxBlockSize.get()
            - MAX_BLOCK_HEADER_SIZE
            - maxNonceSize.get()
            - MAX_COINBASE_TRANSACTION_SIZE
            - Long.BYTES;
    }

    public boolean immatureCoinbase(int chainHeight, UnspentOutputInfo info) {
        return info.isCoinbase() && chainHeight - coinbaseMaturityDepth.get() < info.getBlockHeight();
    }

    public int getMaxBlockSize() {
        return maxBlockSize.get();
    }

    public int getMaxNonceSize() {
        return maxNonceSize.get();
    }

    public int getCoinbaseMaturityDepth() {
        return coinbaseMaturityDepth.get();
    }

    public int getBlockReward() {
        return blockReward.get();
    }

    public int getMinimumTransactionFee() {
        return minimumTransactionFee.get();
    }

    public Hash getTargetValue() {
        return cachedTargetValue;
    }

}
