package org.brabocoin.brabocoin.wallet;

import net.badata.protobuf.converter.annotation.ProtoClass;
import net.badata.protobuf.converter.annotation.ProtoField;
import org.brabocoin.brabocoin.model.Hash;
import org.brabocoin.brabocoin.model.Transaction;
import org.brabocoin.brabocoin.model.proto.ProtoBuilder;
import org.brabocoin.brabocoin.model.proto.ProtoModel;
import org.brabocoin.brabocoin.proto.dal.BrabocoinStorageProtos;
import org.jetbrains.annotations.NotNull;

/**
 * Record of a transaction part of the transaction history in the wallet.
 */
@ProtoClass(BrabocoinStorageProtos.ConfirmedTransaction.class)
public class ConfirmedTransaction implements ProtoModel<ConfirmedTransaction> {

    /**
     * The transaction.
     */
    @ProtoField
    private final @NotNull Transaction transaction;

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
     *     The block height the transaction is recorded at.
     */
    public ConfirmedTransaction(@NotNull Transaction transaction, int blockHeight) {
        this.transaction = transaction;
        this.blockHeight = blockHeight;
    }

    @Override
    public Class<? extends ProtoBuilder> getBuilder() {
        return Builder.class;
    }

    public @NotNull Hash getHash() {
        return transaction.getHash();
    }

    @ProtoClass(BrabocoinStorageProtos.ConfirmedTransaction.class)
    public static class Builder implements ProtoBuilder<ConfirmedTransaction> {

        @ProtoField
        private Transaction.Builder transaction;

        @ProtoField
        private int blockHeight;

        public Builder setTransaction(@NotNull Transaction.Builder transaction) {
            this.transaction = transaction;
            return this;
        }

        public Builder setBlockHeight(int blockHeight) {
            this.blockHeight = blockHeight;
            return this;
        }

        @Override
        public ConfirmedTransaction build() {
            return new ConfirmedTransaction(transaction.build(), blockHeight);
        }
    }
}
