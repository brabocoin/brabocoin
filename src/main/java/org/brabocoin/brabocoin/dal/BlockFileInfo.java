package org.brabocoin.brabocoin.dal;

import net.badata.protobuf.converter.annotation.ProtoClass;
import net.badata.protobuf.converter.annotation.ProtoField;
import org.brabocoin.brabocoin.proto.dal.BrabocoinStorageProtos;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

/**
 * Data class holding block file information that is stored in the blocks database.
 *
 * @see BlockDatabase
 */
@ProtoClass(BrabocoinStorageProtos.BlockFileInfo.class)
public class BlockFileInfo {

    /**
     * Number of full blocks that are stored in this file.
     */
    @ProtoField
    private int numberOfBlocks;

    /**
     * Size of the file in bytes.
     */
    @ProtoField
    private long size;

    /**
     * Minimum block height of all the blocks stored in the file.
     */
    @ProtoField
    private long lowestBlockHeight;

    /**
     * Maximum block height of all the blocks stored in the file.
     */
    @ProtoField
    private long highestBlockHeight;

    /**
     * Earliest timestamp of all the blocks stored in the file.
     */
    @ProtoField
    private long lowestBlockTimestamp;

    /**
     * Latest timestamp of all the blocks stored in the file.
     */
    @ProtoField
    private long highestBlockTimestamp;

    /**
     * Creates a new block file information holder.
     *
     * @param numberOfBlocks
     *         Number of full blocks that are stored in this file.
     * @param size
     *         Size of the file in bytes.
     * @param lowestBlockHeight
     *         Minimum block height of all the blocks stored in the file.
     * @param highestBlockHeight
     *         Maximum block height of all the blocks stored in the file.
     * @param lowestBlockTimestamp
     *         Earliest timestamp of all the blocks stored in the file.
     * @param highestBlockTimestamp
     *         Latest timestamp of all the blocks stored in the file.
     */
    public BlockFileInfo(int numberOfBlocks, long size, long lowestBlockHeight,
                         long highestBlockHeight, long lowestBlockTimestamp,
                         long highestBlockTimestamp) {
        this.numberOfBlocks = numberOfBlocks;
        this.size = size;
        this.lowestBlockHeight = lowestBlockHeight;
        this.highestBlockHeight = highestBlockHeight;
        this.lowestBlockTimestamp = lowestBlockTimestamp;
        this.highestBlockTimestamp = highestBlockTimestamp;
    }

    /**
     * Create a new block file information holder for an empty file.
     * <p>
     * The actual data for the empty holder should only be temporary. Files should only be
     * created and recorded when at least one block is saved to the file.
     *
     * @return The new block file information for an empty file.
     */
    @Contract(" -> new")
    public static @NotNull BlockFileInfo createEmpty() {
        return new Builder().setNumberOfBlocks(0).setSize(0).setLowestBlockHeight(Long.MAX_VALUE).setHighestBlockHeight(0).setLowestBlockTimestamp(Long.MAX_VALUE).setHighestBlockTimestamp(0).createBlockFileInfo();
    }

    public int getNumberOfBlocks() {
        return numberOfBlocks;
    }

    public long getSize() {
        return size;
    }

    public long getLowestBlockHeight() {
        return lowestBlockHeight;
    }

    public long getHighestBlockHeight() {
        return highestBlockHeight;
    }

    public long getLowestBlockTimestamp() {
        return lowestBlockTimestamp;
    }

    public long getHighestBlockTimestamp() {
        return highestBlockTimestamp;
    }

    @ProtoClass(BrabocoinStorageProtos.BlockFileInfo.class)
    public static class Builder {
        @ProtoField
        private int numberOfBlocks;
        @ProtoField
        private long size;
        @ProtoField
        private long lowestBlockHeight;
        @ProtoField
        private long highestBlockHeight;
        @ProtoField
        private long lowestBlockTimestamp;
        @ProtoField
        private long highestBlockTimestamp;

        public Builder setNumberOfBlocks(int numberOfBlocks) {
            this.numberOfBlocks = numberOfBlocks;
            return this;
        }

        public Builder setSize(long size) {
            this.size = size;
            return this;
        }

        public Builder setLowestBlockHeight(long lowestBlockHeight) {
            this.lowestBlockHeight = lowestBlockHeight;
            return this;
        }

        public Builder setHighestBlockHeight(long highestBlockHeight) {
            this.highestBlockHeight = highestBlockHeight;
            return this;
        }

        public Builder setLowestBlockTimestamp(long lowestBlockTimestamp) {
            this.lowestBlockTimestamp = lowestBlockTimestamp;
            return this;
        }

        public Builder setHighestBlockTimestamp(long highestBlockTimestamp) {
            this.highestBlockTimestamp = highestBlockTimestamp;
            return this;
        }

        public BlockFileInfo createBlockFileInfo() {
            return new BlockFileInfo(numberOfBlocks, size, lowestBlockHeight, highestBlockHeight, lowestBlockTimestamp, highestBlockTimestamp);
        }
    }
}
