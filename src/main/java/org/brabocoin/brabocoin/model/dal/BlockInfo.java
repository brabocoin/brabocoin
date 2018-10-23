package org.brabocoin.brabocoin.model.dal;

import com.google.protobuf.ByteString;
import net.badata.protobuf.converter.annotation.ProtoClass;
import net.badata.protobuf.converter.annotation.ProtoField;
import org.brabocoin.brabocoin.dal.BlockDatabase;
import org.brabocoin.brabocoin.model.Hash;
import org.brabocoin.brabocoin.model.proto.ProtoBuilder;
import org.brabocoin.brabocoin.model.proto.ProtoModel;
import org.brabocoin.brabocoin.proto.dal.BrabocoinStorageProtos;
import org.jetbrains.annotations.NotNull;

/**
 * Data class holding block information that is stored in the blocks database.
 *
 * @see BlockDatabase
 */
@ProtoClass(BrabocoinStorageProtos.BlockInfo.class)
public class BlockInfo implements ProtoModel<BlockInfo> {

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
    @ProtoField
    private final ByteString nonce;

    /**
     * UNIX timestamp indicating when the block is created.
     */
    @ProtoField
    private final long timestamp;

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
     * Indicates whether the block has been validated and is considered part of the verified
     * blockchain.
     */
    @ProtoField
    private final boolean validated;

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
     * @param timestamp
     *     UNIX timestamp indicating when the block is created.
     * @param blockHeight
     *     Height of the block in the blockchain.
     * @param transactionCount
     *     The number of transactions in the block, including the coinbase
     *     transaction.
     * @param validated
     *     Indicates whether the block has been validated and is considered part of
     *     the verified blockchain.
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
                     @NotNull Hash targetValue, @NotNull ByteString nonce, long timestamp,
                     int blockHeight, int transactionCount, boolean validated, int fileNumber,
                     int offsetInFile, int sizeInFile, int offsetInUndoFile,
                     int sizeInUndoFile) {
        this.previousBlockHash = previousBlockHash;
        this.merkleRoot = merkleRoot;
        this.targetValue = targetValue;
        this.nonce = nonce;
        this.timestamp = timestamp;
        this.blockHeight = blockHeight;
        this.transactionCount = transactionCount;
        this.validated = validated;
        this.fileNumber = fileNumber;
        this.offsetInFile = offsetInFile;
        this.sizeInFile = sizeInFile;
        this.offsetInUndoFile = offsetInUndoFile;
        this.sizeInUndoFile = sizeInUndoFile;
    }

    public int getBlockHeight() {
        return blockHeight;
    }

    public int getTransactionCount() {
        return transactionCount;
    }

    public boolean isValidated() {
        return validated;
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

    public ByteString getNonce() {
        return nonce;
    }

    public long getTimestamp() {
        return timestamp;
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
        private Hash.Builder previousBlockHash;

        @ProtoField
        private Hash.Builder merkleRoot;

        @ProtoField
        private Hash.Builder targetValue;

        @ProtoField
        private ByteString nonce;

        @ProtoField
        private long timestamp;

        @ProtoField
        private int blockHeight;

        @ProtoField
        private int transactionCount;

        @ProtoField
        private boolean validated;

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

        public Builder setNonce(@NotNull ByteString nonce) {
            this.nonce = nonce;
            return this;
        }

        public Builder setTimestamp(long timestamp) {
            this.timestamp = timestamp;
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

        public Builder setValidated(boolean validated) {
            this.validated = validated;
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
                nonce,
                timestamp,
                blockHeight,
                transactionCount,
                validated,
                fileNumber,
                offsetInFile,
                sizeInFile, offsetInUndoFile, sizeInUndoFile
            );
        }
    }
}
