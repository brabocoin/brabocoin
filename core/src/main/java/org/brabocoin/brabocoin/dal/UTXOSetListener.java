package org.brabocoin.brabocoin.dal;

import org.brabocoin.brabocoin.model.Hash;
import org.brabocoin.brabocoin.model.dal.UnspentOutputInfo;
import org.jetbrains.annotations.NotNull;

/**
 * Listener for UTXO set events.
 */
public interface UTXOSetListener {

    /**
     * Called when a transaction output is marked as unspent.
     *
     * @param transactionHash
     *     The hash of the transaction the output belongs to.
     * @param outputIndex
     *     The index of the output in the transaction.
     * @param info
     *     The unspent info.
     */
    default void onOutputUnspent(@NotNull Hash transactionHash, int outputIndex,
                                 @NotNull UnspentOutputInfo info) {

    }

    /**
     * Called when a transaction output is marked as spent.
     *
     * @param transactionHash
     *     The hash of the transaction the output belongs to.
     * @param outputIndex
     *     The index of the output in the transaction.
     */
    default void onOutputSpent(@NotNull Hash transactionHash, int outputIndex) {

    }
}
