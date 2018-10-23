package org.brabocoin.brabocoin.model.dal;

import net.badata.protobuf.converter.annotation.ProtoClass;
import net.badata.protobuf.converter.annotation.ProtoField;
import org.brabocoin.brabocoin.dal.UTXODatabase;
import org.brabocoin.brabocoin.model.Hash;
import org.brabocoin.brabocoin.model.proto.ProtoBuilder;
import org.brabocoin.brabocoin.model.proto.ProtoModel;
import org.brabocoin.brabocoin.proto.dal.BrabocoinStorageProtos;

/**
 * Data class holding unspent output information that is stored in the blocks database.
 *
 * @see UTXODatabase
 */
@ProtoClass(BrabocoinStorageProtos.UnspentOutputInfo.class)
public class UnspentOutputInfo implements ProtoModel<UnspentOutputInfo> {

    /**
     * Whether the output is from a coinbase transaction.
     */
    @ProtoField
    private boolean coinbase;

    /**
     * The height of the block where the transaction of this output is recorded.
     */
    @ProtoField
    private int blockHeight;

    /**
     * The amount of brabocoin for this output.
     */
    @ProtoField
    private long amount;

    /**
     * The address to which this output is directed.
     */
    @ProtoField
    private Hash address;

    /**
     * Creates a new unspent output information holder.
     *
     * @param coinbase
     *         Whether the output is from a coinbase transaction.
     * @param blockHeight
     *         The height of the block where the transaction of this output is recorded.
     * @param amount
     *         The amount of brabocoin for this output.
     * @param address
     *         The address to which this output is directed.
     */
    public UnspentOutputInfo(boolean coinbase, int blockHeight, long amount, Hash address) {
        this.coinbase = coinbase;
        this.blockHeight = blockHeight;
        this.amount = amount;
        this.address = address;
    }

    /**
     * Whether the output is from a coinbase transaction.
     */
    public boolean isCoinbase() {
        return coinbase;
    }

    /**
     * The height of the block where the transaction of this output is recorded.
     */
    public int getBlockHeight() {
        return blockHeight;
    }

    /**
     * The amount of brabocoin for this output.
     */
    public long getAmount() {
        return amount;
    }

    /**
     * The address to which this output is directed.
     */
    public Hash getAddress() {
        return address;
    }

    @Override
    public Class<? extends ProtoBuilder> getBuilder() {
        return Builder.class;
    }

    @ProtoClass(BrabocoinStorageProtos.UnspentOutputInfo.class)
    public static class Builder implements ProtoBuilder<UnspentOutputInfo> {

        @ProtoField
        private boolean coinbase;

        @ProtoField
        private int blockHeight;

        @ProtoField
        private long amount;

        @ProtoField
        private Hash.Builder address;

        public Builder setCoinbase(boolean coinbase) {
            this.coinbase = coinbase;
            return this;
        }

        public Builder setBlockHeight(int blockHeight) {
            this.blockHeight = blockHeight;
            return this;
        }

        public Builder setAmount(long amount) {
            this.amount = amount;
            return this;
        }

        public Builder setAddress(Hash.Builder address) {
            this.address = address;
            return this;
        }

        @Override
        public UnspentOutputInfo build() {
            return new UnspentOutputInfo(coinbase, blockHeight, amount, address.build());
        }
    }
}
