package org.brabocoin.brabocoin.wallet;

import net.badata.protobuf.converter.annotation.ProtoClass;
import net.badata.protobuf.converter.annotation.ProtoField;
import org.brabocoin.brabocoin.listeners.TransactionHistoryListener;
import org.brabocoin.brabocoin.model.Hash;
import org.brabocoin.brabocoin.model.proto.ConfirmedTransactionMapEntryConverter;
import org.brabocoin.brabocoin.model.proto.ProtoBuilder;
import org.brabocoin.brabocoin.model.proto.ProtoModel;
import org.brabocoin.brabocoin.model.proto.UnconfirmedTransactionMapEntryConverter;
import org.brabocoin.brabocoin.proto.dal.BrabocoinStorageProtos;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Transaction history for wallet.
 */
@ProtoClass(BrabocoinStorageProtos.TransactionHistory.class)
public class TransactionHistory implements ProtoModel<TransactionHistory> {

    private final Set<TransactionHistoryListener> listeners;

    /**
     * Index of confirmed transactions.
     * <p>
     * Confirmed transactions are transactions that are recorded in a block on the main chain.
     */
    @ProtoField(converter = ConfirmedTransactionMapEntryConverter.class)
    private final @NotNull Map<Hash, ConfirmedTransaction> confirmedTransactions;

    /**
     * Index of unconfirmed transactions.
     * <p>
     * Unconfirmed transactions are known transactions, but are not recorded in a block on the
     * main chain.
     */
    @ProtoField(converter = UnconfirmedTransactionMapEntryConverter.class)
    private final @NotNull Map<Hash, UnconfirmedTransaction> unconfirmedTransactions;

    /**
     * Create a new transaction history from the given confirmed and unconfirmed transactions.
     *
     * @param confirmedTransactions
     *     Index of confirmed transactions.
     * @param unconfirmedTransactions
     *     Index of unconfirmed transactions.
     */
    public TransactionHistory(@NotNull Map<Hash, ConfirmedTransaction> confirmedTransactions,
                              @NotNull Map<Hash, UnconfirmedTransaction> unconfirmedTransactions) {
        this.listeners = new HashSet<>();
        this.confirmedTransactions = new HashMap<>(confirmedTransactions);
        this.unconfirmedTransactions = new HashMap<>(unconfirmedTransactions);
    }

    public void addListener(@NotNull TransactionHistoryListener listener) {
        listeners.add(listener);
    }

    public void removeListener(@NotNull TransactionHistoryListener listener) {
        listeners.remove(listener);
    }

    public Map<Hash, ConfirmedTransaction> getConfirmedTransactions() {
        return Collections.unmodifiableMap(confirmedTransactions);
    }

    public Map<Hash, UnconfirmedTransaction> getUnconfirmedTransactions() {
        return Collections.unmodifiableMap(unconfirmedTransactions);
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
    public @Nullable UnconfirmedTransaction findUnconfirmedTransaction(@NotNull Hash hash) {
        return unconfirmedTransactions.get(hash);
    }

    /**
     * Add a transaction to the confirmed transaction index.
     *
     * @param transaction
     *     The transaction to add.
     */
    public void addConfirmedTransaction(@NotNull ConfirmedTransaction transaction) {
        if (confirmedTransactions.containsKey(transaction.getHash())) {
            return;
        }

        confirmedTransactions.put(transaction.getHash(), transaction);
        listeners.forEach(l -> l.onConfirmedTransactionAdded(transaction));
    }

    /**
     * Add a transaction to the unconfirmed transaction index.
     *
     * @param transaction
     *     The transaction to add.
     */
    public void addUnconfirmedTransaction(@NotNull UnconfirmedTransaction transaction) {
        if (unconfirmedTransactions.containsKey(transaction.getHash())) {
            return;
        }

        unconfirmedTransactions.put(transaction.getHash(), transaction);
        listeners.forEach(l -> l.onUnconfirmedTransactionAdded(transaction));
    }

    /**
     * Remove a transaction to the confirmed transaction index.
     *
     * @param hash
     *     The hash of the transaction to remove.
     */
    public void removeConfirmedTransaction(@NotNull Hash hash) {
        ConfirmedTransaction transaction = confirmedTransactions.remove(hash);
        if (transaction != null) {
            listeners.forEach(l -> l.onConfirmedTransactionRemoved(transaction));
        }
    }

    /**
     * Remove a transaction to the unconfirmed transaction index.
     *
     * @param hash
     *     The hash of the transaction to remove.
     */
    public void removeUnconfirmedTransaction(@NotNull Hash hash) {
        UnconfirmedTransaction transaction = unconfirmedTransactions.remove(hash);
        if(transaction != null) {
            listeners.forEach(l -> l.onUnconfirmedTransactionRemoved(transaction));
        }
    }

    @Override
    public Class<? extends ProtoBuilder> getBuilder() {
        return Builder.class;
    }

    @ProtoClass(BrabocoinStorageProtos.TransactionHistory.class)
    public static class Builder implements ProtoBuilder<TransactionHistory> {

        @ProtoField(converter = ConfirmedTransactionMapEntryConverter.class)
        private Map<Hash, ConfirmedTransaction> confirmedTransactions;

        @ProtoField(converter = UnconfirmedTransactionMapEntryConverter.class)
        private Map<Hash, UnconfirmedTransaction> unconfirmedTransactions;

        public Builder setConfirmedTransactions(
            Map<Hash, ConfirmedTransaction> confirmedTransactions) {
            this.confirmedTransactions = confirmedTransactions;
            return this;
        }

        public Builder setUnconfirmedTransactions(
            Map<Hash, UnconfirmedTransaction> unconfirmedTransactions) {
            this.unconfirmedTransactions = unconfirmedTransactions;
            return this;
        }

        @Override
        public TransactionHistory build() {
            return new TransactionHistory(confirmedTransactions, unconfirmedTransactions);
        }
    }

    /**
     * Returns whether no transactions are saved in the transaction history.
     *
     * @return Whether no transactions are in the history.
     */
    public boolean isEmpty() {
        return confirmedTransactions.isEmpty() && unconfirmedTransactions.isEmpty();
    }
}
