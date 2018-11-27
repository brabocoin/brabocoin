package org.brabocoin.brabocoin.chain;

import org.jetbrains.annotations.NotNull;

/**
 * Listener for blockchain events.
 */
public interface BlockchainListener {

    /**
     * Called when a new top block is connected on the main chain.
     *
     * @param block The block that is connected.
     */
    default void onTopBlockConnected(@NotNull IndexedBlock block) {

    }

    /**
     * Called when the current top block is disconnected from the main chain.
     *
     * @param block The block that was disconnected.
     */
    default void onTopBlockDisconnected(@NotNull IndexedBlock block) {

    }
}
