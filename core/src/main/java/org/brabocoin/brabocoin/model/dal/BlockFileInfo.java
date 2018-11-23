package org.brabocoin.brabocoin.model.dal;

import net.badata.protobuf.converter.annotation.ProtoClass;
import net.badata.protobuf.converter.annotation.ProtoField;
import org.brabocoin.brabocoin.dal.BlockDatabase;
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
    private int size;

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
     * Creates a new block file information holder.
     *
     * @param numberOfBlocks
     *     Number of full blocks that are stored in this file.
     * @param size
     *     Size of the file in bytes.
     * @param lowestBlockHeight
     *     Minimum block height of all the blocks stored in the file.
     * @param highestBlockHeight
     *     Maximum block height of all the blocks stored in the file.
     */
    public BlockFileInfo(int numberOfBlocks, int size, int lowestBlockHeight,
                         int highestBlockHeight) {
        this.numberOfBlocks = numberOfBlocks;
        this.size = size;
        this.lowestBlockHeight = lowestBlockHeight;
        this.highestBlockHeight = highestBlockHeight;
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
        return new Builder()
            .setNumberOfBlocks(0)
            .setSize(0)
            .setLowestBlockHeight(Integer.MAX_VALUE)
            .setHighestBlockHeight(0)
            .build();
    }

    public int getNumberOfBlocks() {
        return numberOfBlocks;
    }

    public int getSize() {
        return size;
    }

    public int getLowestBlockHeight() {
        return lowestBlockHeight;
    }

    public int getHighestBlockHeight() {
        return highestBlockHeight;
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
        private int size;

        @ProtoField
        private int lowestBlockHeight;

        @ProtoField
        private int highestBlockHeight;

        public Builder setNumberOfBlocks(int numberOfBlocks) {
            this.numberOfBlocks = numberOfBlocks;
            return this;
        }

        public Builder setSize(int size) {
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

        @Override
        public BlockFileInfo build() {
            return new BlockFileInfo(numberOfBlocks, size, lowestBlockHeight, highestBlockHeight);
        }
    }
}
