package org.brabocoin.brabocoin.dal;

import com.google.protobuf.ByteString;
import net.badata.protobuf.converter.annotation.ProtoClass;
import net.badata.protobuf.converter.annotation.ProtoField;
import org.brabocoin.brabocoin.model.Hash;
import org.brabocoin.brabocoin.proto.dal.BrabocoinStorageProtos;
import org.jetbrains.annotations.NotNull;

/**
 * Data class holding block information that is stored in the blocks database.
 *
 * @see BlockDatabase
 */
@ProtoClass(BrabocoinStorageProtos.BlockInfo.class)
public class BlockInfo {

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
     * UNIX timestamp indicating when the block is created.
     */
    @ProtoField
    private final long timestamp;

    /**
     * Height of the block in the blockchain.
     */
    @ProtoField
    private final long blockHeight;

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
     * The file number in which the full block is stored on disk.
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
    private final long offsetInFile;

    /**
     * The size in bytes of the serialized block in the file.
     *
     * @see BlockFileInfo
     */
    @ProtoField
    private final int sizeInFile;

    /**
     * Creates a new block information holder.
     *
     * @param previousBlockHash Hash of the previous block in the blockchain.
     * @param merkleRoot Hash of the Merkle root of this block.
     * @param targetValue Target value of the proof-of-work of this block.
     * @param nonce Nonce used for the proof-of-work.
     * @param timestamp UNIX timestamp indicating when the block is created.
     * @param blockHeight Height of the block in the blockchain.
     * @param transactionCount The number of transactions in the block, including the coinbase transaction.
     * @param validated Indicates whether the block has been validated and is considered part of
     *                  the verified blockchain.
     * @param fileNumber The file number in which the full block is stored on disk.
     * @param offsetInFile The offset in bytes indicating the location in the file where the full block is stored.
     * @param sizeInFile The size of the serialized block in the file.
     */
    public BlockInfo(@NotNull Hash previousBlockHash, @NotNull Hash merkleRoot,
                     @NotNull Hash targetValue, @NotNull ByteString nonce, long timestamp,
                     long blockHeight, int transactionCount, boolean validated, int fileNumber,
                     long offsetInFile, int sizeInFile) {
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
    }

    public long getBlockHeight() {
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

    public long getOffsetInFile() {
        return offsetInFile;
    }

    public int getSizeInFile() {
        return sizeInFile;
    }

}
