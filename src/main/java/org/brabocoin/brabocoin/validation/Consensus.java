package org.brabocoin.brabocoin.validation;

import com.google.protobuf.ByteString;
import org.brabocoin.brabocoin.Constants;
import org.brabocoin.brabocoin.chain.IndexedBlock;
import org.brabocoin.brabocoin.crypto.Hashing;
import org.brabocoin.brabocoin.model.Block;
import org.brabocoin.brabocoin.model.Hash;
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
    private final @NotNull Block genesisBlock = new Block(
        new Hash(ByteString.EMPTY),
        new Hash(ByteString.copyFromUtf8("root")), // TODO: Merkle root needs implementation
        new Hash(ByteString.copyFromUtf8("easy")), // TODO: Determine target value
        BigInteger.ZERO,
        0,
        Collections.emptyList()
    );

    private final BigInteger maxNonce = new BigInteger("7FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF", 16);

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

    public @NotNull BigInteger getMaxNonce() {
        return maxNonce;
    }

    public @NotNull Function<Hash, Hash> getMerkleHashingFunction() {
        return hash -> Hashing.digestSHA256(Hashing.digestSHA256(hash));
    }

    public int getMaxNonceSize() {
        return 16;
    }

    public Hash getTargetValue() {
        return Hashing.digestSHA256(ByteString.copyFromUtf8("easy"));
    }

    public int getMaxBlockSize() {
        return 1_000_000;
    }

    public int getMaxBlockHeaderSize() {
        return 132;
    }

    public int getMaxCoinbaseTransactionSize() {
        return 36;
    }
}
