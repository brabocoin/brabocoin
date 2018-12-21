package org.brabocoin.brabocoin.chain;

import org.brabocoin.brabocoin.model.Block;
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

    /**
     * Called when a block is added as orphan.
     *
     * @param block The block that was added as orphan.
     */
    default void onOrphanAdded(@NotNull Block block) {

    }

    /**
     * Called when a block is removed as orphan.
     *
     * @param block The block that was removed as orphan.
     */
    default void onOrphanRemoved(@NotNull Block block) {

    }

    /**
     * Called when a block is added as recently rejected.
     *
     * @param block The block that was added as recent reject.
     */
    default void onRecentRejectAdded(@NotNull Block block) {

    }
}
