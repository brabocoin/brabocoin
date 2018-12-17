package org.brabocoin.brabocoin.listeners;

import org.brabocoin.brabocoin.model.Block;

public interface BlockReceivedListener {
    void receivedBlock(Block block);
}
