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
@ProtoClass(BrabocoinStorageProtos.UnconfirmedTransaction.class)
public class UnconfirmedTransaction implements ProtoModel<UnconfirmedTransaction> {

    /**
     * The transaction.
     */
    @ProtoField
    private final @NotNull Transaction transaction;

    /**
     * UNIX timestamp (in seconds) indicating when the transaction was received, in UTC time zone.
     */
    @ProtoField
    private final long timeReceived;

    /**
     * Net amount spent by this user (over all addresses) when this transaction was recorded.
     */
    @ProtoField
    private final long amount;

    /**
     * Create a wallet transaction recorded in the wallet transaction history.
     *
     * @param transaction
     *     The transaction to record.
     * @param timeReceived
     *     The time the transaction has been received by the node.
     * @param amount
     *     Net amount spent by this user (over all addresses).
     */
    public UnconfirmedTransaction(@NotNull Transaction transaction, long timeReceived,
                                  long amount) {
        this.transaction = transaction;
        this.timeReceived = timeReceived;
        this.amount = amount;
    }

    @Override
    public Class<? extends ProtoBuilder> getBuilder() {
        return Builder.class;
    }

    public @NotNull Hash getHash() {
        return transaction.getHash();
    }

    public long getAmount() {
        return amount;
    }

    public long getTimeReceived() {
        return timeReceived;
    }

    public @NotNull Transaction getTransaction() {
        return transaction;
    }

    @ProtoClass(BrabocoinStorageProtos.UnconfirmedTransaction.class)
    public static class Builder implements ProtoBuilder<UnconfirmedTransaction> {

        @ProtoField
        protected Transaction.Builder transaction;

        @ProtoField
        protected long timeReceived;

        @ProtoField
        protected long amount;

        public Builder setTransaction(@NotNull Transaction.Builder transaction) {
            this.transaction = transaction;
            return this;
        }

        public Builder setTimeReceived(long timeReceived) {
            this.timeReceived = timeReceived;
            return this;
        }

        public Builder setAmount(long amount) {
            this.amount = amount;
            return this;
        }

        @Override
        public UnconfirmedTransaction build() {
            return new UnconfirmedTransaction(transaction.build(), timeReceived, amount);
        }
    }
}
