package org.brabocoin.brabocoin.model;

import com.google.protobuf.ByteString;
import net.badata.protobuf.converter.annotation.ProtoClass;
import net.badata.protobuf.converter.annotation.ProtoField;
import org.brabocoin.brabocoin.crypto.Hashing;
import org.brabocoin.brabocoin.model.proto.BigIntegerByteStringConverter;
import org.brabocoin.brabocoin.model.proto.ProtoBuilder;
import org.brabocoin.brabocoin.model.proto.ProtoModel;
import org.brabocoin.brabocoin.proto.model.BrabocoinProtos;
import org.brabocoin.brabocoin.util.ByteUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.math.BigInteger;
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
     * Number indicating the network this block belongs to.
     */
    @ProtoField
    private final int networkId;

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
    @ProtoField(converter = BigIntegerByteStringConverter.class)
    private final @NotNull BigInteger nonce;

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
     * The cached hash of this block.
     */
    protected Hash hash;

    /**
     * Create a new block.
     *
     * @param previousBlockHash
     *     Hash of the previous block in the blockchain.
     * @param merkleRoot
     *     Hash of the Merkle root.
     * @param targetValue
     *     Target value for the proof-of-work.
     * @param nonce
     *     Nonce for the proof-of-work.
     * @param blockHeight
     *     Height of the block in the blockchain.
     * @param transactions
     *     List of transactions contained in this block.
     * @param networkId
     *     Number indicating the network this block belongs to.
     */
    public Block(@NotNull Hash previousBlockHash, @NotNull Hash merkleRoot,
                 @NotNull Hash targetValue, @NotNull BigInteger nonce, int blockHeight,
                 List<Transaction> transactions, int networkId) {
        this.previousBlockHash = previousBlockHash;
        this.merkleRoot = merkleRoot;
        this.targetValue = targetValue;
        this.nonce = nonce;
        this.blockHeight = blockHeight;
        this.transactions = new ArrayList<>(transactions);
        this.networkId = networkId;
    }

    /**
     * Gets the coinbase transaction, which is the first transaction by convention.
     *
     * @return The coinbase transaction.
     */
    public @Nullable Transaction getCoinbaseTransaction() {
        if (getTransactions().size() > 0) {
            return getTransactions().get(0);
        }
        return null;
    }

    /**
     * Gets the block hash using lazy computation if not available.
     * <p>
     * The hash of a block is the hashed output of the block header data only.
     * The hash is computed by applying the SHA-256 hashing function twice.
     *
     * @return The block hash.
     */
    public synchronized @NotNull Hash getHash() {
        if (hash == null) {
            ByteString header = getRawHeader();
            hash = Hashing.digestSHA256(Hashing.digestSHA256(header));
        }
        return hash;
    }

    @NotNull
    protected ByteString getRawHeader() {
        return ByteUtil.toByteString(getNetworkId())
            .concat(getPreviousBlockHash().getValue())
            .concat(getMerkleRoot().getValue())
            .concat(getTargetValue().getValue())
            .concat(ByteUtil.toByteString(getBlockHeight()))
            .concat(ByteString.copyFrom(getNonce().toByteArray()));
    }

    public int getNetworkId() {
        return networkId;
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

    public @NotNull BigInteger getNonce() {
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
        private int networkId;
        @ProtoField
        private Hash.Builder previousBlockHash;
        @ProtoField
        private Hash.Builder merkleRoot;
        @ProtoField
        private Hash.Builder targetValue;
        @ProtoField(converter = BigIntegerByteStringConverter.class)
        private BigInteger nonce;
        @ProtoField
        private int blockHeight;
        @ProtoField
        private List<Transaction.Builder> transactions;

        public Builder setNetworkId(int networkId) {
            this.networkId = networkId;
            return this;
        }

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

        public Builder setNonce(BigInteger nonce) {
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
            return new Block(
                previousBlockHash.build(),
                merkleRoot.build(),
                targetValue.build(),
                nonce,
                blockHeight,
                transactions.stream()
                    .map(Transaction.Builder::build)
                    .collect(Collectors.toList()),
                networkId
            );
        }
    }
}
