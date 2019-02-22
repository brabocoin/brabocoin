package org.brabocoin.brabocoin.listeners;

import org.brabocoin.brabocoin.wallet.ConfirmedTransaction;
import org.brabocoin.brabocoin.wallet.TransactionHistory;
import org.brabocoin.brabocoin.wallet.UnconfirmedTransaction;

/**
 * Listener for {@link TransactionHistory}.
 */
public interface TransactionHistoryListener {

    default void onConfirmedTransactionAdded(ConfirmedTransaction transaction) {

    }

    default void onConfirmedTransactionRemoved(ConfirmedTransaction transaction) {

    }

    default void onUnconfirmedTransactionAdded(UnconfirmedTransaction transaction) {

    }

    default void onUnconfirmedTransactionRemoved(UnconfirmedTransaction transaction) {

    }
}
