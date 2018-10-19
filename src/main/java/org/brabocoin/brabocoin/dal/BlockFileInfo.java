package org.brabocoin.brabocoin.dal;

import net.badata.protobuf.converter.annotation.ProtoClass;
import net.badata.protobuf.converter.annotation.ProtoField;
import org.brabocoin.brabocoin.model.proto.ProtoBuilder;
import org.brabocoin.brabocoin.model.proto.ProtoModel;
import org.brabocoin.brabocoin.proto.dal.BrabocoinStorageProtos;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

/**
 * Data class holding block file information that is stored in the blocks database.
 *
 * @see BlockDatabase
 */
@ProtoClass(BrabocoinStorageProtos.BlockFileInfo.class)
public class BlockFileInfo implements ProtoModel<BlockFileInfo> {

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
    private int lowestBlockHeight;

    /**
     * Maximum block height of all the blocks stored in the file.
     */
    @ProtoField
    private int highestBlockHeight;

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
    public BlockFileInfo(int numberOfBlocks, long size, int lowestBlockHeight,
                         int highestBlockHeight, long lowestBlockTimestamp,
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
        return new Builder().setNumberOfBlocks(0).setSize(0).setLowestBlockHeight(Integer.MAX_VALUE).setHighestBlockHeight(0).setLowestBlockTimestamp(Long.MAX_VALUE).setHighestBlockTimestamp(0).build();
    }

    public int getNumberOfBlocks() {
        return numberOfBlocks;
    }

    public long getSize() {
        return size;
    }

    public int getLowestBlockHeight() {
        return lowestBlockHeight;
    }

    public int getHighestBlockHeight() {
        return highestBlockHeight;
    }

    public long getLowestBlockTimestamp() {
        return lowestBlockTimestamp;
    }

    public long getHighestBlockTimestamp() {
        return highestBlockTimestamp;
    }

    @Override
    public Class<? extends ProtoBuilder> getBuilder() {
        return Builder.class;
    }

    @ProtoClass(BrabocoinStorageProtos.BlockFileInfo.class)
    public static class Builder implements ProtoBuilder<BlockFileInfo> {

        @ProtoField
        private int numberOfBlocks;

        @ProtoField
        private long size;

        @ProtoField
        private int lowestBlockHeight;

        @ProtoField
        private int highestBlockHeight;

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

        public Builder setLowestBlockHeight(int lowestBlockHeight) {
            this.lowestBlockHeight = lowestBlockHeight;
            return this;
        }

        public Builder setHighestBlockHeight(int highestBlockHeight) {
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

        @Override
        public BlockFileInfo build() {
            return new BlockFileInfo(numberOfBlocks, size, lowestBlockHeight, highestBlockHeight, lowestBlockTimestamp, highestBlockTimestamp);
        }
    }
}
