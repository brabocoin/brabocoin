package org.brabocoin.brabocoin.listeners;

public interface UpdateBlockchainListener {
    void onStartUpdate();

    void onUpdateFinished();
}
