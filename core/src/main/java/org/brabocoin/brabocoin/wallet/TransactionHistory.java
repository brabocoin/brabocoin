package org.brabocoin.brabocoin.wallet;

import net.badata.protobuf.converter.annotation.ProtoClass;
import net.badata.protobuf.converter.annotation.ProtoField;
import org.brabocoin.brabocoin.model.Hash;
import org.brabocoin.brabocoin.model.Transaction;
import org.brabocoin.brabocoin.model.proto.ProtoBuilder;
import org.brabocoin.brabocoin.model.proto.ProtoModel;
import org.brabocoin.brabocoin.proto.dal.BrabocoinStorageProtos;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Transaction history for wallet.
 */
@ProtoClass(BrabocoinStorageProtos.TransactionHistory.class)
public class TransactionHistory implements ProtoModel<TransactionHistory> {

    /**
     * Index of confirmed transactions.
     * <p>
     * Confirmed transactions are transactions that are recorded in a block on the main chain.
     */
    private final @NotNull Map<Hash, ConfirmedTransaction> confirmedTransactions;

    /**
     * Index of unconfirmed transactions.
     * <p>
     * Unconfirmed transactions are known transactions, but are not recorded in a block on the
     * main chain.
     */
    private final @NotNull Map<Hash, Transaction> unconfirmedTransactions;

    /**
     * Create a new transaction history from the given confirmed and unconfirmed transactions.
     *
     * @param confirmedTransactions
     *     Index of confirmed transactions.
     * @param unconfirmedTransactions
     *     Index of unconfirmed transactions.
     */
    public TransactionHistory(@NotNull Map<Hash, ConfirmedTransaction> confirmedTransactions,
                              @NotNull Map<Hash, Transaction> unconfirmedTransactions) {
        this.confirmedTransactions = new HashMap<>(confirmedTransactions);
        this.unconfirmedTransactions = new HashMap<>(unconfirmedTransactions);
    }

    /**
     * Find a confirmed transaction.
     *
     * @param hash
     *     The hash of the transaction to find.
     * @return The confirmed transaction, or {@code null} if the transaction is not present in
     * the confirmed transaction index.
     */
    public @Nullable ConfirmedTransaction findConfirmedTransaction(@NotNull Hash hash) {
        return confirmedTransactions.get(hash);
    }

    /**
     * Find an unconfirmed transaction.
     *
     * @param hash
     *     The hash of the transaction to find.
     * @return The unconfirmed transaction, or {@code null} if the transaction is not present in
     * the unconfirmed transaction index.
     */
    public @Nullable Transaction findUnconfirmedTransaction(@NotNull Hash hash) {
        return unconfirmedTransactions.get(hash);
    }

    /**
     * Add a transaction to the confirmed transaction index.
     *
     * @param transaction
     *     The transaction to add.
     */
    public void addConfirmedTransaction(@NotNull ConfirmedTransaction transaction) {
        confirmedTransactions.put(transaction.getHash(), transaction);
    }

    /**
     * Add a transaction to the unconfirmed transaction index.
     *
     * @param transaction
     *     The transaction to add.
     */
    public void addUnconfirmedTransaction(@NotNull Transaction transaction) {
        unconfirmedTransactions.put(transaction.getHash(), transaction);
    }

    /**
     * Remove a transaction to the confirmed transaction index.
     *
     * @param hash
     *     The hash of the transaction to remove.
     */
    public void removeConfirmedTransaction(@NotNull Hash hash) {
        confirmedTransactions.remove(hash);
    }

    /**
     * Remove a transaction to the unconfirmed transaction index.
     *
     * @param hash
     *     The hash of the transaction to remove.
     */
    public void removeUnconfirmedTransaction(@NotNull Hash hash) {
        unconfirmedTransactions.remove(hash);
    }

    @Override
    public Class<? extends ProtoBuilder> getBuilder() {
        return Builder.class;
    }

    @ProtoClass(BrabocoinStorageProtos.TransactionHistory.class)
    public static class Builder implements ProtoBuilder<TransactionHistory> {

        @ProtoField
        private Map<Hash.Builder, ConfirmedTransaction.Builder> confirmedTransactions;

        @ProtoField
        private Map<Hash.Builder, Transaction.Builder> unconfirmedTransactions;

        public Builder setConfirmedTransactions(
            Map<Hash.Builder, ConfirmedTransaction.Builder> confirmedTransactions) {
            this.confirmedTransactions = confirmedTransactions;
            return this;
        }

        public Builder setUnconfirmedTransactions(
            Map<Hash.Builder, Transaction.Builder> unconfirmedTransactions) {
            this.unconfirmedTransactions = unconfirmedTransactions;
            return this;
        }

        @Override
        public TransactionHistory build() {
            return new TransactionHistory(
                confirmedTransactions.entrySet().stream().collect(
                    Collectors.toMap(e -> e.getKey().build(), e -> e.getValue().build())
                ),
                unconfirmedTransactions.entrySet().stream().collect(
                    Collectors.toMap(e -> e.getKey().build(), e -> e.getValue().build())
                )
            );
        }
    }
}
