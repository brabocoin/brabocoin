package org.brabocoin.brabocoin.processor;

import org.brabocoin.brabocoin.model.Block;
import org.jetbrains.annotations.NotNull;

/**
 * Listener for block processor events.
 */
public interface BlockProcessorListener {

    /**
     * Called when synchronization of the main chain with the chain UTXO set has started.
     *
     * @see BlockProcessor#syncMainChainWithUTXOSet()
     */
    default void onSyncWithUTXOSetStarted() {

    }

    /**
     * Called when synchronization of the main chain with the chain UTXO set has finished.
     *
     * @see BlockProcessor#syncMainChainWithUTXOSet()
     */
    default void onSyncWithUTXOSetFinished() {

    }

    /**
     * Called when a block is processed and added to the blockchain as
     * {@link org.brabocoin.brabocoin.validation.ValidationStatus#VALID}.
     *
     * @param block The block that was added.
     *
     * @see BlockProcessor#processNewBlock(Block)
     */
    default void onValidBlockProcessed(@NotNull Block block){

    }
}
