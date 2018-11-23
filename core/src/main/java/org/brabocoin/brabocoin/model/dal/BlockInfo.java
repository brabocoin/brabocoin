package org.brabocoin.brabocoin.model.dal;

import net.badata.protobuf.converter.annotation.ProtoClass;
import net.badata.protobuf.converter.annotation.ProtoField;
import org.brabocoin.brabocoin.dal.BlockDatabase;
import org.brabocoin.brabocoin.model.Hash;
import org.brabocoin.brabocoin.model.proto.BigIntegerByteStringConverter;
import org.brabocoin.brabocoin.model.proto.ProtoBuilder;
import org.brabocoin.brabocoin.model.proto.ProtoModel;
import org.brabocoin.brabocoin.proto.dal.BrabocoinStorageProtos;
import org.jetbrains.annotations.NotNull;

import java.math.BigInteger;

/**
 * Data class holding block information that is stored in the blocks database.
 *
 * @see BlockDatabase
 */
@ProtoClass(BrabocoinStorageProtos.BlockInfo.class)
public class BlockInfo implements ProtoModel<BlockInfo> {

    /**
     * Number indicating the network this block belongs to.
     */
    @ProtoField
    private final int networkId;

    /**
     * Hash of the previous block in the blockchain.
     */
    @ProtoField
    private final Hash previousBlockHash;

    /**
     * Hash of the Merkle root of this block.
     */
    @ProtoField
    private final Hash merkleRoot;

    /**
     * Target value of the proof-of-work of this block.
     */
    @ProtoField
    private final Hash targetValue;

    /**
     * Nonce used for the proof-of-work.
     */
    @ProtoField(converter = BigIntegerByteStringConverter.class)
    private final BigInteger nonce;

    /**
     * UNIX timestamp (in seconds) indicating when the block was received, in UTC time zone.
     */
    @ProtoField
    private final long timeReceived;

    /**
     * Height of the block in the blockchain.
     */
    @ProtoField
    private final int blockHeight;

    /**
     * The number of transactions in the block, including the coinbase transaction.
     */
    @ProtoField
    private final int transactionCount;

    /**
     * Indicates whether the block is considered valid.
     * <p>
     * Note: this does not guarantee that the block is indeed fully valid, as multiple checks must
     * be performed that are context-dependent. When this field is {@code false}, the block is
     * definitely invalid and cannot become valid at a later time.
     */
    @ProtoField
    private final boolean valid;

    /**
     * The file number in which the full block is stored on disk, as well as the number of the
     * undo file.
     *
     * @see BlockFileInfo
     */
    @ProtoField
    private final int fileNumber;

    /**
     * The offset in bytes indicating the location in the file where the full block is stored.
     *
     * @see BlockFileInfo
     */
    @ProtoField
    private final int offsetInFile;

    /**
     * The size in bytes of the serialized block in the file.
     *
     * @see BlockFileInfo
     */
    @ProtoField
    private final int sizeInFile;

    /**
     * The offset in bytes indicating the location in the file where the undo data is stored.
     */
    @ProtoField
    private final int offsetInUndoFile;

    /**
     * The size in bytes of the serialized undo data in the file.
     */
    @ProtoField
    private final int sizeInUndoFile;

    /**
     * Creates a new block information holder.
     *
     * @param previousBlockHash
     *     Hash of the previous block in the blockchain.
     * @param merkleRoot
     *     Hash of the Merkle root of this block.
     * @param targetValue
     *     Target value of the proof-of-work of this block.
     * @param nonce
     *     Nonce used for the proof-of-work.
     * @param blockHeight
     *     Height of the block in the blockchain.
     * @param transactionCount
     *     The number of transactions in the block, including the coinbase
     *     transaction.
     * @param networkId
     *     Number indicating the network this block belongs to.
     * @param valid
     *     Indicates whether the block is considered valid.
     * @param timeReceived
     *     UNIX timestamp (in seconds) indicating when the block was received, in UTC time zone.
     * @param fileNumber
     *     The file number in which the full block is stored on disk.
     * @param offsetInFile
     *     The offset in bytes indicating the location in the file where the full block is stored.
     * @param sizeInFile
     *     The size of the serialized block in the file.
     * @param offsetInUndoFile
     *     The offset in bytes indicating the location in the file where the undo data is stored.
     * @param sizeInUndoFile
     *     The size in bytes of the serialized undo data in the file.
     */
    public BlockInfo(@NotNull Hash previousBlockHash, @NotNull Hash merkleRoot,
                     @NotNull Hash targetValue, @NotNull BigInteger nonce, int blockHeight,
                     int transactionCount, int networkId, boolean valid, long timeReceived,
                     int fileNumber, int offsetInFile, int sizeInFile, int offsetInUndoFile,
                     int sizeInUndoFile) {
        this.previousBlockHash = previousBlockHash;
        this.merkleRoot = merkleRoot;
        this.targetValue = targetValue;
        this.nonce = nonce;
        this.blockHeight = blockHeight;
        this.transactionCount = transactionCount;
        this.networkId = networkId;
        this.timeReceived = timeReceived;
        this.valid = valid;
        this.fileNumber = fileNumber;
        this.offsetInFile = offsetInFile;
        this.sizeInFile = sizeInFile;
        this.offsetInUndoFile = offsetInUndoFile;
        this.sizeInUndoFile = sizeInUndoFile;
    }

    public int getNetworkId() {
        return networkId;
    }

    public int getBlockHeight() {
        return blockHeight;
    }

    public int getTransactionCount() {
        return transactionCount;
    }

    public boolean isValid() {
        return valid;
    }

    public long getTimeReceived() {
        return timeReceived;
    }

    public int getFileNumber() {
        return fileNumber;
    }

    public int getOffsetInFile() {
        return offsetInFile;
    }

    public int getSizeInFile() {
        return sizeInFile;
    }

    public Hash getPreviousBlockHash() {
        return previousBlockHash;
    }

    public Hash getMerkleRoot() {
        return merkleRoot;
    }

    public Hash getTargetValue() {
        return targetValue;
    }

    public BigInteger getNonce() {
        return nonce;
    }

    public int getOffsetInUndoFile() {
        return offsetInUndoFile;
    }

    public int getSizeInUndoFile() {
        return sizeInUndoFile;
    }

    @Override
    public Class<? extends ProtoBuilder> getBuilder() {
        return Builder.class;
    }

    @ProtoClass(BrabocoinStorageProtos.BlockInfo.class)
    public static class Builder implements ProtoBuilder<BlockInfo> {

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
        private long timeReceived;

        @ProtoField
        private int blockHeight;

        @ProtoField
        private int transactionCount;

        @ProtoField
        private boolean valid;

        @ProtoField
        private int fileNumber;

        @ProtoField
        private int offsetInFile;

        @ProtoField
        private int sizeInFile;

        @ProtoField
        private int offsetInUndoFile;

        @ProtoField
        private int sizeInUndoFile;

        public Builder setNetworkId(int networkId) {
            this.networkId = networkId;
            return this;
        }

        public Builder setPreviousBlockHash(@NotNull Hash.Builder previousBlockHash) {
            this.previousBlockHash = previousBlockHash;
            return this;
        }

        public Builder setMerkleRoot(@NotNull Hash.Builder merkleRoot) {
            this.merkleRoot = merkleRoot;
            return this;
        }

        public Builder setTargetValue(@NotNull Hash.Builder targetValue) {
            this.targetValue = targetValue;
            return this;
        }

        public Builder setNonce(@NotNull BigInteger nonce) {
            this.nonce = nonce;
            return this;
        }

        public Builder setTimeReceived(long timeReceived) {
            this.timeReceived = timeReceived;
            return this;
        }

        public Builder setBlockHeight(int blockHeight) {
            this.blockHeight = blockHeight;
            return this;
        }

        public Builder setTransactionCount(int transactionCount) {
            this.transactionCount = transactionCount;
            return this;
        }

        public Builder setValid(boolean valid) {
            this.valid = valid;
            return this;
        }

        public Builder setFileNumber(int fileNumber) {
            this.fileNumber = fileNumber;
            return this;
        }

        public Builder setOffsetInFile(int offsetInFile) {
            this.offsetInFile = offsetInFile;
            return this;
        }

        public Builder setSizeInFile(int sizeInFile) {
            this.sizeInFile = sizeInFile;
            return this;
        }

        public Builder setOffsetInUndoFile(int offsetInUndoFile) {
            this.offsetInUndoFile = offsetInUndoFile;
            return this;
        }

        public Builder setSizeInUndoFile(int sizeInUndoFile) {
            this.sizeInUndoFile = sizeInUndoFile;
            return this;
        }

        @Override
        public BlockInfo build() {
            return new BlockInfo(
                previousBlockHash.build(),
                merkleRoot.build(),
                targetValue.build(),
                nonce, blockHeight,
                transactionCount,
                networkId,
                valid,
                timeReceived,
                fileNumber,
                offsetInFile,
                sizeInFile,
                offsetInUndoFile,
                sizeInUndoFile
            );
        }
    }
}
