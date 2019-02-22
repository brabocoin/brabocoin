package org.brabocoin.brabocoin.listeners;

import org.brabocoin.brabocoin.model.Block;
import org.brabocoin.brabocoin.model.Transaction;

public interface NotificationListener {
    void receivedBlock(Block block);

    void receivedTransaction(Transaction transaction);

    void forkSwitched(int height);
}
