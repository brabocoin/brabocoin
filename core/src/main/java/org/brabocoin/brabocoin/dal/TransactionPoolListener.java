package org.brabocoin.brabocoin.dal;

import org.brabocoin.brabocoin.model.Transaction;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

/**
 * Listener for transaction pool events.
 */
public interface TransactionPoolListener {

    /**
     * Called when a transaction is added to the transaction pool, either as dependent or
     * independent.
     * <p>
     * Note that when a transaction is added as orphan, this event will not fire. Use
     * {@link #onTransactionAddedAsOrphan(Transaction)} for this purpose.
     *
     * @param transaction
     *     The transaction that is added to the pool.
     */
    default void onTransactionAddedToPool(@NotNull Transaction transaction) {

    }

    /**
     * Called when a transaction is added as orphan.
     *
     * @param transaction
     *     The transaction that is added as orphan.
     */
    default void onTransactionAddedAsOrphan(@NotNull Transaction transaction) {

    }

    /**
     * Called when a transaction was removed from the transaction pool.
     * <p>
     * This can be triggered either when the transaction is removed because it is now recorded in
     * a block on the main chain, or when the transaction is removed because of size limiting.
     *
     * @param transaction
     *     The transaction that was removed from the transaction pool.
     */
    default void onTransactionRemovedFromPool(@NotNull Transaction transaction) {

    }

    /**
     * Called when a transaction was removed from the transaction pool.
     * <p>
     * This can be triggered either when the transaction is removed because it is now in the
     * transaction pool (as dependent or independent), or when the transaction is removed because
     * of size limiting.
     *
     * @param transaction
     *     The transaction that was removed from the orphan pool.
     */
    default void onTransactionRemovedAsOrphan(@NotNull Transaction transaction) {

    }

    /**
     * Called when a transaction is added as recently rejected.
     *
     * @param transaction
     *     The transaction that was added as recent reject.
     */
    default void onRecentRejectAdded(@NotNull Transaction transaction) {

    }

    /**
     * Called when the transactions are promoted from dependent to independent.
     *
     * @param transactions
     *     The transactions that were promoted.
     */
    default void onDependentTransactionsPromoted(@NotNull Collection<Transaction> transactions) {

    }

    /**
     * Called when the transactions are demoted from independent to dependent.
     *
     * @param transactions
     *     The transactions that were demoted.
     */
    default void onIndependentTransactionsDemoted(@NotNull Collection<Transaction> transactions) {

    }
}
