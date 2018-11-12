package org.brabocoin.brabocoin.model;

import com.google.protobuf.ByteString;
import net.badata.protobuf.converter.annotation.ProtoClass;
import net.badata.protobuf.converter.annotation.ProtoField;
import org.brabocoin.brabocoin.crypto.Hashing;
import org.brabocoin.brabocoin.model.proto.ProtoBuilder;
import org.brabocoin.brabocoin.model.proto.ProtoModel;
import org.brabocoin.brabocoin.proto.model.BrabocoinProtos;
import org.brabocoin.brabocoin.util.ByteUtil;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * A collection of transactions, part of the blockchain.
 */
@ProtoClass(BrabocoinProtos.Block.class)
public class Block implements ProtoModel<Block> {

    /**
     * Hash of the previous block in the blockchain.
     */
    @ProtoField
    private final @NotNull Hash previousBlockHash;

    /**
     * Hash of the Merkle root of this block.
     */
    @ProtoField
    private final @NotNull Hash merkleRoot;

    /**
     * Target value of the proof-of-work of this block.
     */
    @ProtoField
    private final @NotNull Hash targetValue;

    /**
     * Nonce used for the proof-of-work.
     */
    @ProtoField
    private final @NotNull ByteString nonce;

    /**
     * The height of the block in the block chain.
     */
    @ProtoField
    private final int blockHeight;

    /**
     * The transactions contained in this block.
     */
    @ProtoField
    private final List<Transaction> transactions;

    /**
     * Create a new block.
     *
     * @param previousBlockHash
     *         Hash of the previous block in the blockchain.
     * @param merkleRoot
     *         Hash of the Merkle root.
     * @param targetValue
     *         Target value for the proof-of-work.
     * @param nonce
     *         Nonce for the proof-of-work.
     * @param blockHeight
     *         Height of the block in the blockchain.
     * @param transactions
     *         List of transactions contained in this block.
     */
    public Block(@NotNull Hash previousBlockHash, @NotNull Hash merkleRoot, @NotNull Hash targetValue, @NotNull ByteString nonce, int blockHeight, List<Transaction> transactions) {
        this.previousBlockHash = previousBlockHash;
        this.merkleRoot = merkleRoot;
        this.targetValue = targetValue;
        this.nonce = nonce;
        this.blockHeight = blockHeight;
        this.transactions = new ArrayList<>(transactions);
    }

    /**
     * Computes the block hash.
     * <p>
     * The hash of a block is the hashed output of the block header data only.
     * The hash is computed by applying the SHA-256 hashing function twice.
     *
     * @return The block hash.
     */
    public @NotNull Hash computeHash() {
        ByteString header = getRawHeader();
        return Hashing.digestSHA256(Hashing.digestSHA256(header));
    }

    private @NotNull ByteString getRawHeader() {
        return previousBlockHash.getValue()
                .concat(merkleRoot.getValue())
                .concat(targetValue.getValue())
                .concat(ByteUtil.toByteString(blockHeight))
                .concat(nonce);
    }

    public @NotNull Hash getPreviousBlockHash() {
        return previousBlockHash;
    }

    public @NotNull Hash getMerkleRoot() {
        return merkleRoot;
    }

    public @NotNull Hash getTargetValue() {
        return targetValue;
    }

    public @NotNull ByteString getNonce() {
        return nonce;
    }

    public int getBlockHeight() {
        return blockHeight;
    }

    public List<Transaction> getTransactions() {
        return Collections.unmodifiableList(transactions);
    }

    @Override
    public Class<? extends ProtoBuilder<Block>> getBuilder() {
        return Builder.class;
    }

    @ProtoClass(BrabocoinProtos.Block.class)
    public static class Builder implements ProtoBuilder<Block> {

        @ProtoField
        private Hash.Builder previousBlockHash;
        @ProtoField
        private Hash.Builder merkleRoot;
        @ProtoField
        private Hash.Builder targetValue;
        @ProtoField
        private ByteString nonce;
        @ProtoField
        private int blockHeight;
        @ProtoField
        private List<Transaction.Builder> transactions;

        public Builder setPreviousBlockHash(Hash.Builder previousBlockHash) {
            this.previousBlockHash = previousBlockHash;
            return this;
        }

        public Builder setMerkleRoot(Hash.Builder merkleRoot) {
            this.merkleRoot = merkleRoot;
            return this;
        }

        public Builder setTargetValue(Hash.Builder targetValue) {
            this.targetValue = targetValue;
            return this;
        }

        public Builder setNonce(ByteString nonce) {
            this.nonce = nonce;
            return this;
        }

        public Builder setBlockHeight(int blockHeight) {
            this.blockHeight = blockHeight;
            return this;
        }

        @SuppressWarnings("AssignmentOrReturnOfFieldWithMutableType")
        public void setTransactions(List<Transaction.Builder> transactions) {
            this.transactions = transactions;
        }

        @Override
        public Block build() {
            return new Block(previousBlockHash.build(),
                    merkleRoot.build(),
                    targetValue.build(),
                    nonce, blockHeight,
                    transactions.stream()
                            .map(Transaction.Builder::build)
                            .collect(Collectors.toList())
            );
        }
    }
}
