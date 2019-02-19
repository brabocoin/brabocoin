package org.brabocoin.brabocoin.wallet;

import net.badata.protobuf.converter.annotation.ProtoClass;
import net.badata.protobuf.converter.annotation.ProtoField;
import org.brabocoin.brabocoin.model.Transaction;
import org.brabocoin.brabocoin.model.proto.ProtoBuilder;
import org.brabocoin.brabocoin.proto.dal.BrabocoinStorageProtos;
import org.jetbrains.annotations.NotNull;

/**
 * Record of a transaction part of the transaction history in the wallet.
 */
@ProtoClass(BrabocoinStorageProtos.ConfirmedTransaction.class)
public class ConfirmedTransaction extends UnconfirmedTransaction {

    /**
     * The block height the transaction is recorded at.
     */
    @ProtoField
    private final int blockHeight;

    /**
     * Create a wallet transaction recorded in the wallet transaction history.
     *
     * @param transaction
     *     The transaction to record.
     * @param blockHeight
     *     The height of the block in the main chain this transaction is part of.
     * @param timeReceived
     *     The time the transaction has been received by the node.
     * @param amount
     *     Net amount spent by this user (over all addresses).
     */
    public ConfirmedTransaction(@NotNull Transaction transaction, int blockHeight,
                                long timeReceived, long amount) {
        super(transaction, timeReceived, amount);
        this.blockHeight = blockHeight;
    }

    @Override
    public Class<? extends ProtoBuilder> getBuilder() {
        return Builder.class;
    }

    public int getBlockHeight() {
        return blockHeight;
    }

    @ProtoClass(BrabocoinStorageProtos.ConfirmedTransaction.class)
    public static class Builder extends UnconfirmedTransaction.Builder {

        @ProtoField
        private int blockHeight;

        public Builder setBlockHeight(int blockHeight) {
            this.blockHeight = blockHeight;
            return this;
        }

        @Override
        public ConfirmedTransaction build() {
            return new ConfirmedTransaction(transaction.build(), blockHeight, timeReceived, amount);
        }
    }
}
