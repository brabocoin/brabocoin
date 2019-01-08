package org.brabocoin.brabocoin.listeners;

import org.brabocoin.brabocoin.model.Transaction;
import org.brabocoin.brabocoin.wallet.ConfirmedTransaction;
import org.brabocoin.brabocoin.wallet.TransactionHistory;

/**
 * Listener for {@link TransactionHistory}.
 */
public interface TransactionHistoryListener {

    default void onConfirmedTransactionAdded(ConfirmedTransaction transaction) {

    }

    default void onConfirmedTransactionRemoved(ConfirmedTransaction transaction) {

    }

    default void onUnconfirmedTransactionAdded(Transaction transaction) {

    }

    default void onUnconfirmedTransactionRemoved(Transaction transaction) {

    }
}
