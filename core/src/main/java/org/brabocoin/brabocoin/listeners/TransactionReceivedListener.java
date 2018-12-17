package org.brabocoin.brabocoin.listeners;

import org.brabocoin.brabocoin.model.Transaction;

public interface TransactionReceivedListener {
    void receivedTransaction(Transaction transaction);
}
