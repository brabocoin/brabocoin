package org.brabocoin.brabocoin.processor;

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
}
