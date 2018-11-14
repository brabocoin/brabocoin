package org.brabocoin.brabocoin.validation;

import com.google.protobuf.ByteString;
import org.brabocoin.brabocoin.Constants;
import org.brabocoin.brabocoin.chain.IndexedBlock;
import org.brabocoin.brabocoin.model.Block;
import org.brabocoin.brabocoin.model.Hash;
import org.brabocoin.brabocoin.model.Output;
import org.brabocoin.brabocoin.model.Transaction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;

/**
 * Consensus rules.
 */
public class Consensus {
    private final @NotNull Block genesisBlock = new Block(
        new Hash(ByteString.EMPTY),
        new Hash(ByteString.copyFromUtf8("root")), // TODO: Merkle root needs implementation
        new Hash(ByteString.copyFromUtf8("easy")), // TODO: Determine target value
        ByteString.copyFromUtf8("genesis"),
        0,
        Arrays.asList(
            new Transaction(
                Collections.emptyList(),
                Arrays.asList(
                    new Output(new Hash(ByteString.copyFromUtf8("address1")), 10),
                    new Output(new Hash(ByteString.copyFromUtf8("address2")), 10)
                )
            )
        )
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
                .getBlockHeight()).thenComparing(Comparator.<IndexedBlock, BigInteger>comparing(b -> new BigInteger(b.getHash().getValue().toByteArray())).reversed()))
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

}
